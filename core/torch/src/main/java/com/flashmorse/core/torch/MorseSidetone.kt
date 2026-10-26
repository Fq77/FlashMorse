package com.flashmorse.core.torch

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * 摩斯侧音：600 Hz 连续正弦，灯亮即响、灯灭即停。
 *
 * 实现要点（都来自真实踩坑）：
 * 1. **音轨必须预创建**。在时序线程首次发声时才构建 AudioTrack
 *    （create + write 耗时数十毫秒），`waitUntil` 落后后事件连续追赶，
 *    表现为「屏幕一直快速闪、滴声跟着错」。因此构造即创建。
 * 2. **亮/灭切换用 setVolume，不用 pause/play**。pause 与 play 在部分设备上
 *    有可感知延迟，同样会拖慢时序线程；音量切换是即时的混音参数更新。
 *    轨道常驻播放（静态缓冲无限循环），静音即「灭」。
 *
 * 频率取 600 Hz——短波摩斯监听的传统侧音频率，点划长短由灯态自然区分。
 */
class MorseSidetone {

    private var track: AudioTrack? = null

    /** 点亮：出声。重复调用幂等。 */
    fun on() {
        val t = track ?: return
        if (t.playState != AudioTrack.PLAYSTATE_PLAYING) {
            runCatching { t.play() }
        }
        t.setVolume(OUTPUT_VOLUME)
    }

    /** 熄灭：静音。重复调用幂等。 */
    fun off() {
        track?.setVolume(0f)
    }

    fun release() {
        runCatching { track?.release() }
        track = null
    }

    private fun create(): AudioTrack {
        val sampleRate = 44_100
        val bufferMs = 40
        val frameCount = sampleRate * bufferMs / 1000
        val samples = ShortArray(frameCount) { i ->
            (sin(2.0 * PI * TONE_HZ * i / sampleRate) * Short.MAX_VALUE * OUTPUT_VOLUME).toInt().toShort()
        }
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(frameCount * 2)
            .build()
            .apply {
                write(samples, 0, samples.size)
                // -1 = 无限循环：静态缓冲的内容持续重播，形成连续音
                setLoopPoints(0, frameCount, -1)
            }
    }

    private companion object {
        const val TONE_HZ = 600.0
        const val OUTPUT_VOLUME = 0.6f
    }
}
