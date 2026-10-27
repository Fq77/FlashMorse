package com.flashmorse.core.torch.impl

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.util.Log
import com.flashmorse.core.model.Brightness
import com.flashmorse.core.model.TransmissionChannel
import com.flashmorse.core.torch.TorchCapabilities
import com.flashmorse.core.torch.TorchController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 主力方案：`CameraManager.setTorchMode`（API 23+，**无需 CAMERA 权限**）。
 *
 * 这是产品的信任基石——装完即用、不弹权限窗（架构文档 §4.1 / §6.1）。
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

    /** 手电筒被其他应用抢占时发射（架构文档 §4.2 中断检测）。 */
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

    /**
     * 亮度是否真的可调：API 33+ 且 HAL 报出多于一档。
     *
     * 运行期一次调用失败就会降级为 false（见 [applyOn]），UI 依据它决定是否展示亮度项，
     * 而不是让用户滑了没反应。
     */
    override var supportsBrightness: Boolean =
        isApi33() && capabilities.supportsStrength
        private set

    @Volatile
    private var brightnessFraction: Float = 1f

    /** 上一次实际下发的档位，只用于「变化时打一条日志」，避免每个符号刷一行。 */
    @Volatile
    private var lastAppliedLevel: Int = -1

    override fun setBrightness(fraction: Float) {
        brightnessFraction = fraction.coerceIn(0f, 1f)
        // 常亮 / 试闪期间改亮度要立刻可见；发送期间由调度器保证不会调到这里。
        if (requestedOn) applyOn()
    }

    override fun setOn(on: Boolean) {
        Log.d(TAG, "setOn($on) requestedOn=$requestedOn")
        if (!isAvailable) return
        if (on) {
            applyOn()
            return
        }
        if (!requestedOn) return
        requestedOn = false
        runCatching { cameraManager?.setTorchMode(cameraId, false) }
            .onFailure { Log.w(TAG, "setTorchMode(false) failed", it) }
    }

    /**
     * 点亮。
     *
     * 有档位时必须用 [CameraManager.turnOnTorchWithStrengthLevel]，不能用 `setTorchMode(true)`：
     * 后者按硬件**默认档**点亮，会把用户设定的亮度直接冲掉。
     *
     * 关灯仍走 `setTorchMode(false)`——灭灯路径越短越好，不引入额外参数校验。
     */
    private fun applyOn() {
        if (!isAvailable) return
        if (supportsBrightness) {
            val level = Brightness.levelFor(brightnessFraction, capabilities.maxStrengthLevel)
            val applied = runCatching {
                cameraManager?.turnOnTorchWithStrengthLevel(cameraId, level)
                true
            }.getOrElse { error ->
                Log.w(TAG, "turnOnTorchWithStrengthLevel($level) failed, fallback to default", error)
                false
            }
            if (applied) {
                requestedOn = true
                if (level != lastAppliedLevel) {
                    lastAppliedLevel = level
                    Log.d(TAG, "torch on at level $level / ${capabilities.maxStrengthLevel}")
                }
                return
            }
            // 该机型实际不可用（权限被裁剪 / HAL 拒绝）：本次进程内不再尝试，
            // 否则每个符号都会抛一次异常，时序线程会被异常构造拖垮。
            supportsBrightness = false
        }

        runCatching {
            cameraManager?.setTorchMode(cameraId, true)
        }.onSuccess {
            requestedOn = true
        }.onFailure {
            Log.w(TAG, "setTorchMode(true) failed", it)
            requestedOn = false
            isAvailable = false
            _externalTakeover.tryEmit(Unit)
        }
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

        /** 遍历 cameraIdList，选首个「后置 + 有闪光灯」的 ID，绝不硬编码 "0"（架构文档 §4.3）。 */
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
                defaultStrengthLevel = defaultLevel.coerceAtLeast(1),
            )
        }
    }
}
