package com.auraplayer.offline

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Real-time PCM listening effects used by the native player.
 * The source file is never modified; only decoded playback audio is processed.
 */
class ListeningEffects : BaseAudioProcessor() {
    @Volatile var mode: String = "Original"

    private var phase = 0.0
    private var bassL = 0.0
    private var bassR = 0.0
    private var toneL = 0.0
    private var toneR = 0.0
    private var delayL = DoubleArray(1)
    private var delayR = DoubleArray(1)
    private var delayAt = 0
    private var channels = 2
    private var sampleRate = 44100

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount !in 1..2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        channels = inputAudioFormat.channelCount
        sampleRate = inputAudioFormat.sampleRate
        return AudioProcessor.AudioFormat(sampleRate, 2, C.ENCODING_PCM_16BIT)
    }

    override fun onFlush() {
        phase = 0.0
        bassL = 0.0
        bassR = 0.0
        toneL = 0.0
        toneR = 0.0
        delayL = DoubleArray((sampleRate * .105).toInt().coerceAtLeast(1))
        delayR = DoubleArray((sampleRate * .132).toInt().coerceAtLeast(1))
        delayAt = 0
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val frames = inputBuffer.remaining() / (channels * 2)
        val output = replaceOutputBuffer(frames * 4).order(ByteOrder.LITTLE_ENDIAN)

        repeat(frames) {
            val left = inputBuffer.short.toDouble() / 32768.0
            val right = if (channels == 2) inputBuffer.short.toDouble() / 32768.0 else left
            var l = left
            var r = right

            when (mode) {
                "8D", "16D" -> {
                    val speed = if (mode == "8D") .20 else .37
                    val depth = if (mode == "8D") .76 else .90
                    val pan = sin(phase) * depth
                    phase += 2.0 * PI * speed / sampleRate
                    if (phase > 2.0 * PI) phase -= 2.0 * PI
                    val center = (left + right) * .5
                    l = center * cos((pan + 1.0) * PI / 4.0) * 1.15 + left * .10
                    r = center * sin((pan + 1.0) * PI / 4.0) * 1.15 + right * .10
                }

                "3D Surround" -> {
                    val indexL = delayAt % delayL.size
                    val indexR = delayAt % delayR.size
                    val oldL = delayL[indexL]
                    val oldR = delayR[indexR]
                    delayL[indexL] = left
                    delayR[indexR] = right
                    val mid = (left + right) * .5
                    val side = (left - right) * .72
                    l = mid + side + oldR * .14
                    r = mid - side + oldL * .14
                    delayAt++
                }

                "Wide Stereo" -> {
                    val mid = (left + right) * .5
                    val side = (left - right) * .82
                    l = mid + side
                    r = mid - side
                }

                "Bass Boost", "Deep Bass" -> {
                    val deep = mode == "Deep Bass"
                    val cutoff = if (deep) 105.0 else 170.0
                    val alpha = 1.0 - exp(-2.0 * PI * cutoff / sampleRate)
                    bassL += alpha * (left - bassL)
                    bassR += alpha * (right - bassR)
                    val boost = if (deep) 1.22 else .78
                    val gain = if (deep) .82 else .88
                    l = tanh((left + bassL * boost) * gain)
                    r = tanh((right + bassR * boost) * gain)
                }

                "Lo-fi" -> {
                    val alpha = 1.0 - exp(-2.0 * PI * 2900.0 / sampleRate)
                    toneL += alpha * (left - toneL)
                    toneR += alpha * (right - toneR)
                    l = tanh(toneL * 1.55) * .78
                    r = tanh(toneR * 1.55) * .78
                }

                "Reverb" -> {
                    val indexL = delayAt % delayL.size
                    val indexR = delayAt % delayR.size
                    val oldL = delayL[indexL]
                    val oldR = delayR[indexR]
                    delayL[indexL] = left + oldL * .27
                    delayR[indexR] = right + oldR * .27
                    l = left * .82 + oldR * .25
                    r = right * .82 + oldL * .25
                    delayAt++
                }

                "Mono" -> {
                    val center = (left + right) * .5
                    l = center
                    r = center
                }
            }

            output.putShort((l.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort())
            output.putShort((r.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort())
        }

        inputBuffer.position(inputBuffer.limit())
        output.flip()
    }
}
