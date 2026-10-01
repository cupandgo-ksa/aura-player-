package com.auraplayer.offline

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/** Processes decoded PCM before playback. All changes are heard, not just UI presets. */
class ListeningEffects : BaseAudioProcessor() {
    @Volatile var mode: String = "Original"
    private var phase = 0.0
    private var lowL = 0.0
    private var lowR = 0.0
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
        lowL = 0.0
        lowR = 0.0
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
            val preset = mode
            var l = left
            var r = right
            if (preset == "8D" || preset == "16D") {
                val speed = if (preset == "8D") .20 else .37
                val depth = if (preset == "8D") .76 else .90
                val pan = sin(phase) * depth
                phase += 2.0 * PI * speed / sampleRate
                if (phase > 2.0 * PI) phase -= 2.0 * PI
                val center = (left + right) * .5
                l = (center * cos((pan + 1.0) * PI / 4.0) * 1.15 + left * .10).coerceIn(-1.0, 1.0)
                r = (center * sin((pan + 1.0) * PI / 4.0) * 1.15 + right * .10).coerceIn(-1.0, 1.0)
            } else if (preset == "Lo-fi") {
                val alpha = 1.0 - kotlin.math.exp(-2.0 * PI * 2900.0 / sampleRate)
                lowL += alpha * (l - lowL)
                lowR += alpha * (r - lowR)
                l = tanh(lowL * 1.55) * .78
                r = tanh(lowR * 1.55) * .78
            } else if (preset == "Reverb") {
                val oldL = delayL[delayAt % delayL.size]
                val oldR = delayR[delayAt % delayR.size]
                delayL[delayAt % delayL.size] = l + oldL * .27
                delayR[delayAt % delayR.size] = r + oldR * .27
                l = (l * .82 + oldR * .25).coerceIn(-1.0, 1.0)
                r = (r * .82 + oldL * .25).coerceIn(-1.0, 1.0)
                delayAt++
            }
            output.putShort((l.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort())
            output.putShort((r.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort())
        }
        inputBuffer.position(inputBuffer.position() + inputBuffer.remaining())
        output.flip()
    }
}
