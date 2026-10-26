package com.flashmorse.core.torch

import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import com.flashmorse.core.model.AbortReason
import com.flashmorse.core.model.ChannelPreference
import com.flashmorse.core.model.PlaybackState
import com.flashmorse.core.model.SendScript
import com.flashmorse.core.model.TimingProfile
import com.flashmorse.core.model.TransmissionChannel
import com.flashmorse.core.morse.timing.HighResolutionClock
import com.flashmorse.core.torch.impl.ScreenFlashController
import com.flashmorse.core.torch.impl.TorchManagerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.locks.LockSupport

/**
 * 绝对时间轴调度器（规划文档 §5.4 / §6.2）。
 *
 * 与「逐条 delay() 累积」的反面方案相比，本实现：
 * 1. 所有事件时刻都是相对锚点的**绝对偏移**，单次抖动不向后传递；
 * 2. 运行在独立 `HandlerThread`（`THREAD_PRIORITY_URGENT_AUDIO`），不占用协程调度器；
 * 3. 采用「粗睡 + 自旋命中」的混合等待，避免 `parkNanos` 提前唤醒带来的误差。
 *
 * 调度器只认「在 t 时刻把灯设为 on/off」，通道（LED / 屏幕）对上层透明。
 */
class MorseTransmitter(
    private val ledController: TorchController?,
    private val screenController: ScreenFlashController,
    private val clock: HighResolutionClock = AndroidHighResolutionClock,
) {

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _activeChannel = MutableStateFlow(TransmissionChannel.Screen)
    val activeChannel: StateFlow<TransmissionChannel> = _activeChannel.asStateFlow()

    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null

    private val pauseLock = Object()

    @Volatile
    private var runToken: Int = 0

    @Volatile
    private var pauseRequested: Boolean = false

    @Volatile
    private var currentController: TorchController = screenController

    /** LED 常亮开关的本地镜像（`setTorchMode` 不回读状态，需自行记账）。 */
    private var constantLedOn: Boolean = false

    /**
     * 手电筒（常亮）是否点亮。
     *
     * 顶栏的手电筒开关在任意页面都要实时反映灯态，所以这里用 StateFlow
     * 而不是让 UI 每次都来查询。
     */
    private val _torchLit = kotlinx.coroutines.flow.MutableStateFlow(false)
    val torchLit: kotlinx.coroutines.flow.StateFlow<Boolean> = _torchLit.asStateFlow()

    /**
     * 用户设定的手电筒亮度（0..1）。
     *
     * 屏幕通道不参与调光，这里只服务 LED；发送过程中不打断当前时间轴，
     * 等下一次 [play] 生效——见 [setBrightness]。
     */
    @Volatile
    private var brightnessFraction: Float = 1f

    val ledAvailable: Boolean get() = ledController?.isAvailable == true

    /** LED 是否支持真实档位调光（API 33+ 且 HAL 报出多档；调用失败会自行降级）。 */
    val ledSupportsBrightness: Boolean get() = ledController?.supportsBrightness == true

    fun capabilities(): TorchCapabilities? =
        (ledController as? TorchManagerController)?.capabilities

    /**
     * 依据用户偏好与当前速度解析实际通道。
     *
     * 自动模式下，单位时长 < 80 ms（> 15 WPM）时切屏幕通道——LED 的 HAL 延迟
     * 在该速度下不可靠（规划文档 §6.1 重要限制）。
     */
    fun resolveChannel(preference: ChannelPreference, timing: TimingProfile): TransmissionChannel {
        val ledUsable = ledAvailable
        return when (preference) {
            ChannelPreference.Screen -> TransmissionChannel.Screen
            ChannelPreference.Led -> if (ledUsable) TransmissionChannel.Led else TransmissionChannel.Screen
            ChannelPreference.Auto -> when {
                !ledUsable -> TransmissionChannel.Screen
                !timing.isLedReliable -> TransmissionChannel.Screen
                else -> TransmissionChannel.Led
            }
        }
    }

    fun play(script: SendScript, loop: Boolean, channel: TransmissionChannel) {
        if (script.isEmpty) return
        // 亮度在每次起播前下发一次：即便用户是在上一次发送过程中改的值，这里也会追上。
        applyBrightness(brightnessFraction)
        val controller = controllerFor(channel)
        if (!controller.isAvailable) {
            _state.value = PlaybackState.Aborted(AbortReason.TorchUnavailable)
            return
        }

        cancelCurrent(silent = true)
        currentController = controller
        _activeChannel.value = controller.channel

        val token = ++runToken
        _state.value = PlaybackState.Preparing(script)
        ensureWorker()
        workerHandler?.post { runScript(script, loop, token, controller) }
    }

    fun pause() {
        if (_state.value !is PlaybackState.Transmitting) return
        pauseRequested = true
    }

    fun resume() {
        if (!pauseRequested) return
        pauseRequested = false
        synchronized(pauseLock) { pauseLock.notifyAll() }
    }

    fun stop(
        reason: AbortReason = AbortReason.UserStopped,
        silent: Boolean = false,
    ) {
        val wasActive = _state.value.let { it is PlaybackState.Transmitting || it is PlaybackState.Paused || it is PlaybackState.Preparing }
        runToken++
        pauseRequested = false
        synchronized(pauseLock) { pauseLock.notifyAll() }
        safetyOff()
        if (silent) return
        if (!wasActive && reason == AbortReason.UserStopped) return
        _state.value = when (reason) {
            AbortReason.UserStopped -> PlaybackState.Idle
            else -> PlaybackState.Aborted(reason)
        }
    }

    /**
     * 设置发光亮度（0..1）。
     *
     * 发送中只记值、不下发：中途改档会让同一段报文的点划边沿特性突变，
     * 接收端会把"变暗"误判成"变短"。下一次起播时由 [play] 统一应用。
     */
    fun setBrightness(fraction: Float) {
        brightnessFraction = fraction.coerceIn(0f, 1f)
        if (_state.value.isBusy()) return
        applyBrightness(brightnessFraction)
    }

    /** 复位为 Idle（页面消费掉错误提示后调用）。 */
    fun consumeTerminalState() {
        val current = _state.value
        if (current is PlaybackState.Aborted || current is PlaybackState.Completed) {
            _state.value = PlaybackState.Idle
        }
    }

    /** F-04：独立的常亮开关，用于照明与自检；发送期间无效。 */
    fun setConstantLight(on: Boolean, channel: TransmissionChannel = _activeChannel.value) {
        if (_state.value.isBusy()) return
        val controller = controllerFor(channel)
        if (!controller.isAvailable) return
        currentController = controller
        _activeChannel.value = controller.channel
        controller.setOn(on)
        constantLedOn = on && controller.channel == TransmissionChannel.Led
        _torchLit.value = on
    }

    fun isConstantLightOn(): Boolean = when (currentController.channel) {
        TransmissionChannel.Led -> constantLedOn
        TransmissionChannel.Screen -> screenController.lit.value
    }

    /**
     * 手动按键：按下即亮、松开即灭（F-03）。
     *
     * 与 [setConstantLight] 的区别在于它不参与「常亮」记账，只做瞬时通断。
     */
    fun manualKey(down: Boolean, channel: TransmissionChannel): Boolean {
        if (_state.value.isBusy()) return false
        val controller = controllerFor(channel)
        if (!controller.isAvailable) return false
        currentController = controller
        _activeChannel.value = controller.channel
        controller.setOn(down)
        constantLedOn = false
        _torchLit.value = false
        return true
    }

    fun release() {
        stop(silent = true)
        runToken++
        safetyOff()
        workerThread?.quitSafely()
        workerThread = null
        workerHandler = null
        ledController?.release()
        screenController.release()
    }

    // ---------------------------------------------------------------- internals

    private fun runScript(
        script: SendScript,
        loop: Boolean,
        token: Int,
        controller: TorchController,
    ) {
        val compensationNanos = script.timing.compensationMillis * NANOS_PER_MILLI

        // 锚点后移 L_cmd：命令在 (锚点 + offset - L) 下发，光在 (锚点 + offset) 出现，
        // 逻辑时间轴因此与光的实际出现时刻对齐；由于整体平移，点划比例与累积误差指标不受影响。
        var anchorNanos = clock.nanos() + compensationNanos
        val events = script.events

        try {
            while (runToken == token) {
                for (index in events.indices) {
                    if (runToken != token) return
                    if (pauseRequested) {
                        anchorNanos = awaitResume(script, index, token) ?: return
                    }

                    val event = events[index]
                    val targetNanos = anchorNanos + event.offsetNanos - compensationNanos
                    waitUntil(targetNanos, token)
                    if (runToken != token) return

                    controller.setOn(event.on)
                    publishProgress(script, index, loop, targetNanos - anchorNanos)
                }

                controller.setOn(false)

                if (!loop) {
                    if (runToken == token) _state.value = PlaybackState.Completed(script)
                    return
                }

                // 循环发送（SOS）：补一个词间隔后重新锚定，避免重新累积
                val gapNanos = script.timing.wordGapMillis * NANOS_PER_MILLI
                waitUntil(clock.nanos() + gapNanos, token)
                if (runToken != token) return
                anchorNanos = clock.nanos() + compensationNanos
            }
        } finally {
            if (runToken != token) {
                controller.setOn(false)
            }
            if (controller.channel == TransmissionChannel.Led) constantLedOn = false
            _torchLit.value = false
        }
    }

    private fun publishProgress(
        script: SendScript,
        eventIndex: Int,
        loop: Boolean,
        elapsedNanos: Long,
    ) {
        val event = script.events[eventIndex]
        val cell = script.cells.getOrNull(event.cellIndex) ?: return
        val remaining = (script.totalNanos - elapsedNanos).coerceAtLeast(0L)
        val next = PlaybackState.Transmitting(
            script = script,
            currentIndex = event.cellIndex,
            currentSymbol = cell.symbol,
            elapsedMs = elapsedNanos / NANOS_PER_MILLI,
            estimatedRemainingMs = remaining / NANOS_PER_MILLI,
            loop = loop,
            emittedAtNanos = clock.nanos(),
        )
        if (_state.value != next) _state.value = next
    }

    /**
     * 混合等待：先粗睡到目标前 [COARSE_MARGIN_NANOS]，最后几毫秒自旋命中。
     *
     * 纯 `parkNanos` 会因调度器提前唤醒产生毫秒级偏差，纯自旋又会在长间隔上浪费 CPU，
     * 二者结合才能同时满足精度与功耗。
     */
    private fun waitUntil(targetNanos: Long, token: Int) {
        while (runToken == token) {
            val remaining = targetNanos - clock.nanos()
            if (remaining <= 0L) return
            if (remaining > COARSE_THRESHOLD_NANOS) {
                LockSupport.parkNanos(remaining - COARSE_MARGIN_NANOS)
                if (Thread.interrupted()) return
            } else {
                Thread.yield()
            }
        }
    }

    private fun awaitResume(script: SendScript, index: Int, token: Int): Long? {
        synchronized(pauseLock) {
            while (pauseRequested) {
                if (runToken != token) return null
                val paused = PlaybackState.Paused(script, index)
                if (_state.value != paused) _state.value = paused
                runCatching { pauseLock.wait(PAUSE_POLL_MILLIS) }
            }
        }
        if (runToken != token) return null
        val compensationNanos = script.timing.compensationMillis * NANOS_PER_MILLI
        return clock.nanos() - script.events[index].offsetNanos + compensationNanos
    }

    /** 只下发 LED：屏幕通道的价值是"最亮、最容易被看见"，不参与调光。 */
    private fun applyBrightness(fraction: Float) {
        runCatching { ledController?.setBrightness(fraction) }
    }

    private fun controllerFor(channel: TransmissionChannel): TorchController = when (channel) {
        TransmissionChannel.Led -> ledController ?: screenController
        TransmissionChannel.Screen -> screenController
    }

    private fun cancelCurrent(silent: Boolean) {
        runToken++
        pauseRequested = false
        synchronized(pauseLock) { pauseLock.notifyAll() }
        safetyOff()
        if (!silent) _state.value = PlaybackState.Idle
    }

    private fun safetyOff() {
        runCatching { ledController?.setOn(false) }
        runCatching { screenController.setOn(false) }
        constantLedOn = false
        _torchLit.value = false
    }

    private fun ensureWorker() {
        if (workerHandler != null) return
        val thread = HandlerThread("morse-timing", Process.THREAD_PRIORITY_URGENT_AUDIO)
        thread.start()
        workerThread = thread
        workerHandler = Handler(thread.looper)
    }

    private fun PlaybackState.isBusy(): Boolean =
        this is PlaybackState.Preparing || this is PlaybackState.Transmitting || this is PlaybackState.Paused

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L

        /** 超过该阈值走粗睡，否则自旋。 */
        const val COARSE_THRESHOLD_NANOS = 4_000_000L

        /** 粗睡提前量，留出自旋命中窗口。 */
        const val COARSE_MARGIN_NANOS = 2_000_000L

        const val PAUSE_POLL_MILLIS = 120L
    }
}
