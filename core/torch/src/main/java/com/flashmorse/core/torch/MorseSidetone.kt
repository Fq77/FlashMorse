package com.flashmorse.core.torch

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * 摩斯侧音：600 Hz 连续正弦，灯亮即响、灯灭即停。
 *
 * 用 [AudioTrack] 静态缓冲 + 无限循环实现持续音：`play()` / `pause()` 都是
 * 音频框架的轻量调用，不会给时序线程带来可感知的抖动。
 * 频率取 600 Hz——短波摩斯监听的传统侧音频率，点划长短由灯态自然区分，
 * 长时间收听不易疲劳。
 */
class MorseSidetone {

    private var track: AudioTrack? = null

    /** 点亮：开始（或恢复）发声。重复调用幂等。 */
    fun on() {
        val t = track ?: create().also { track = it }
        if (t.playState != AudioTrack.PLAYSTATE_PLAYING) {
            runCatching { t.play() }
        }
    }

    /** 熄灭：静音。重复调用幂等。 */
    fun off() {
        track?.let { t ->
            if (t.playState == AudioTrack.PLAYSTATE_PLAYING) {
                runCatching { t.pause() }
            }
        }
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
            (sin(2.0 * PI * TONE_HZ * i / sampleRate) * Short.MAX_VALUE * VOLUME).toInt().toShort()
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
        const val VOLUME = 0.6
    }
}
