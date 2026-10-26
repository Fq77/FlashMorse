package com.flashmorse.core.torch.impl

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.util.Log
import com.flashmorse.core.model.TransmissionChannel
import com.flashmorse.core.torch.TorchCapabilities
import com.flashmorse.core.torch.TorchController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 主力方案：`CameraManager.setTorchMode`（API 23+，**无需 CAMERA 权限**）。
 *
 * 这是产品的信任基石——装完即用、不弹权限窗（规划文档 §4.1 / §6.1）。
 * 它同时监听 [CameraManager.TorchCallback]，识别「被其他应用抢占」的情况。
 */
class TorchManagerController(
    context: Context,
    val capabilities: TorchCapabilities,
) : TorchController {

    private val appContext = context.applicationContext
    private val cameraManager: CameraManager? =
        appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val cameraId: String = capabilities.cameraId.orEmpty()

    private var callbackThread: HandlerThread? = null
    private var callbackHandler: Handler? = null

    /** 我们期望的灯态；用于区分「自己关灯」与「被外部关灯」。 */
    @Volatile
    private var requestedOn: Boolean = false

    @Volatile
    override var isAvailable: Boolean = capabilities.hasFlash && cameraId.isNotEmpty()
        private set

    override val channel: TransmissionChannel = TransmissionChannel.Led

    private val _externalTakeover = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** 手电筒被其他应用抢占时发射（规划文档 §4.2 中断检测）。 */
    val externalTakeover: SharedFlow<Unit> = _externalTakeover.asSharedFlow()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id != cameraId) return
            // 外部把灯关掉，但我们仍期望它是亮的 —— 判定为被抢占
            if (!enabled && requestedOn) {
                requestedOn = false
                _externalTakeover.tryEmit(Unit)
            }
        }

        override fun onTorchModeUnavailable(id: String) {
            if (id != cameraId) return
            if (requestedOn) {
                requestedOn = false
                _externalTakeover.tryEmit(Unit)
            }
        }
    }

    init {
        if (isAvailable) {
            registerCallback()
        }
    }

    private fun registerCallback() {
        runCatching {
            val thread = HandlerThread("torch-callback", Process.THREAD_PRIORITY_BACKGROUND)
            thread.start()
            callbackThread = thread
            val handler = Handler(thread.looper)
            callbackHandler = handler
            cameraManager?.registerTorchCallback(torchCallback, handler)
        }.onFailure { Log.w(TAG, "registerTorchCallback failed", it) }
    }

    override fun setOn(on: Boolean) {
        if (!isAvailable) return
        if (on == requestedOn && !on) return
        requestedOn = on
        runCatching {
            cameraManager?.setTorchMode(cameraId, on)
        }.onFailure {
            Log.w(TAG, "setTorchMode($on) failed", it)
            if (on) {
                requestedOn = false
                isAvailable = false
                _externalTakeover.tryEmit(Unit)
            }
        }
    }

    /** F-15：仅 API 33+ 且硬件支持时生效，低版本必须由 UI 隐藏该项。 */
    fun setStrength(level: Int): Boolean {
        if (!isAvailable || !isApi33()) return false
        val clamped = level.coerceIn(capabilities.minStrengthLevel, capabilities.maxStrengthLevel)
        return runCatching {
            cameraManager?.turnOnTorchWithStrengthLevel(cameraId, clamped)
            true
        }.getOrDefault(false)
    }

    override fun release() {
        runCatching { cameraManager?.setTorchMode(cameraId, false) }
        runCatching { cameraManager?.unregisterTorchCallback(torchCallback) }
        callbackThread?.quitSafely()
        callbackThread = null
        callbackHandler = null
    }

    private fun isApi33(): Boolean = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU

    companion object {
        private const val TAG = "TorchManager"

        /** 遍历 cameraIdList，选首个「后置 + 有闪光灯」的 ID，绝不硬编码 "0"（规划文档 §4.3）。 */
        fun detect(context: Context): TorchCapabilities {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return TorchCapabilities()
            val ids = runCatching { manager.cameraIdList }.getOrDefault(emptyArray())

            var fallbackId: String? = null
            for (id in ids) {
                val characteristics = runCatching { manager.getCameraCharacteristics(id) }.getOrNull() ?: continue
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (!hasFlash) continue
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return buildCapabilities(id, characteristics)
                }
                if (fallbackId == null) fallbackId = id
            }
            return fallbackId?.let { id ->
                val characteristics = runCatching { manager.getCameraCharacteristics(id) }.getOrNull()
                if (characteristics != null) buildCapabilities(id, characteristics) else TorchCapabilities()
            } ?: TorchCapabilities()
        }

        private fun buildCapabilities(
            id: String,
            characteristics: CameraCharacteristics,
        ): TorchCapabilities {
            val maxLevel = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 0
                }.getOrDefault(0)
            } else {
                0
            }
            val defaultLevel = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL) ?: 1
                }.getOrDefault(1)
            } else {
                1
            }
            return TorchCapabilities(
                cameraId = id,
                hasFlash = true,
                supportsStrength = maxLevel > 1,
                maxStrengthLevel = maxLevel,
                minStrengthLevel = defaultLevel.coerceAtLeast(1),
            )
        }
    }
}
