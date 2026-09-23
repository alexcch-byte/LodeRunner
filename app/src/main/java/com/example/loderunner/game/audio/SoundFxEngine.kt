package com.example.loderunner.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pure Kotlin procedural 8-bit sound synthesizer using AudioTrack.
 * Generates authentic 1980s retro PC sound effects without external audio files.
 */
class SoundFxEngine {
    var isSoundEnabled: Boolean = true
    private val sampleRate = 22050
    private val scope = CoroutineScope(Dispatchers.Default)

    private fun playTone(
        durationMs: Int,
        startFreq: Float,
        endFreq: Float = startFreq,
        waveform: Waveform = Waveform.SQUARE,
        volume: Float = 0.5f,
        noiseMix: Float = 0f
    ) {
        if (!isSoundEnabled) return

        scope.launch {
            try {
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(100)
                val buffer = ShortArray(numSamples)
                var phase = 0.0

                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val currentFreq = startFreq + (endFreq - startFreq) * progress
                    val phaseIncrement = 2.0 * PI * currentFreq / sampleRate
                    phase += phaseIncrement

                    val toneSample = when (waveform) {
                        Waveform.SQUARE -> if (sin(phase) >= 0) 1.0f else -1.0f
                        Waveform.SINE -> sin(phase).toFloat()
                        Waveform.TRIANGLE -> (2.0f / PI.toFloat()) * kotlin.math.asin(sin(phase)).toFloat()
                        Waveform.NOISE -> (Random.nextFloat() * 2f - 1f)
                    }

                    val finalSample = if (noiseMix > 0f) {
                        val noise = (Random.nextFloat() * 2f - 1f)
                        (toneSample * (1f - noiseMix) + noise * noiseMix)
                    } else {
                        toneSample
                    }

                    // Simple envelope: linear fade out to prevent clicks
                    val envelope = (1f - progress).coerceIn(0f, 1f)
                    val sampleVal = (finalSample * envelope * volume * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    buffer[i] = sampleVal.toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()

                // Release track after playback completes
                kotlinx.coroutines.delay(durationMs.toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // Ignore audio play errors on background thread
            }
        }
    }

    private fun playSequence(notes: List<Note>) {
        if (!isSoundEnabled) return
        scope.launch {
            for (note in notes) {
                playTone(
                    durationMs = note.durationMs,
                    startFreq = note.freq,
                    waveform = note.waveform,
                    volume = note.volume
                )
                kotlinx.coroutines.delay(note.durationMs.toLong() + 10)
            }
        }
    }

    fun playDig() {
        // Crunchy retro dig pulse
        playTone(durationMs = 90, startFreq = 220f, endFreq = 90f, waveform = Waveform.SQUARE, volume = 0.45f, noiseMix = 0.4f)
    }

    fun playGold() {
        // Classic high-pitched pickup arpeggio
        playSequence(
            listOf(
                Note(659.25f, 50, Waveform.SQUARE, 0.4f), // E5
                Note(830.61f, 50, Waveform.SQUARE, 0.4f), // G#5
                Note(987.77f, 50, Waveform.SQUARE, 0.4f), // B5
                Note(1318.51f, 90, Waveform.SQUARE, 0.45f) // E6
            )
        )
    }

    fun playFall() {
        // Rapid descending whistle
        playTone(durationMs = 180, startFreq = 700f, endFreq = 180f, waveform = Waveform.SINE, volume = 0.35f)
    }

    fun playTrap() {
        // Enemy trapped squawk
        playTone(durationMs = 120, startFreq = 350f, endFreq = 150f, waveform = Waveform.SQUARE, volume = 0.4f, noiseMix = 0.2f)
    }

    fun playCrush() {
        // Crushed enemy bass crunch
        playTone(durationMs = 240, startFreq = 120f, endFreq = 40f, waveform = Waveform.NOISE, volume = 0.55f)
    }

    fun playVictory() {
        // Retro stage clear fanfare
        playSequence(
            listOf(
                Note(523.25f, 80, Waveform.SQUARE, 0.4f), // C5
                Note(659.25f, 80, Waveform.SQUARE, 0.4f), // E5
                Note(783.99f, 80, Waveform.SQUARE, 0.4f), // G5
                Note(1046.50f, 180, Waveform.SQUARE, 0.45f) // C6
            )
        )
    }

    fun playDeath() {
        // Descending defeat jingle
        playSequence(
            listOf(
                Note(440.00f, 90, Waveform.SQUARE, 0.45f), // A4
                Note(415.30f, 90, Waveform.SQUARE, 0.45f), // G#4
                Note(392.00f, 90, Waveform.SQUARE, 0.45f), // G4
                Note(349.23f, 180, Waveform.SQUARE, 0.45f) // F4
            )
        )
    }

    private data class Note(
        val freq: Float,
        val durationMs: Int,
        val waveform: Waveform = Waveform.SQUARE,
        val volume: Float = 0.4f
    )

    enum class Waveform {
        SQUARE,
        SINE,
        TRIANGLE,
        NOISE
    }
}
