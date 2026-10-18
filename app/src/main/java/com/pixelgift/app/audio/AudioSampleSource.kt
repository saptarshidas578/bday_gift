package com.pixelgift.app.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

interface AudioSampleSource {
    fun start(onSamples: (ShortArray) -> Unit)
    fun stop()
    val isRunning: Boolean
}

/**
 * Captures real-time audio from Android microphone via AudioRecord.
 */
class MicrophoneAudioSource(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    val sampleRate: Int = 44100
) : AudioSampleSource {

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private var _isRunning = false
    override val isRunning: Boolean
        get() = _isRunning

    @SuppressLint("MissingPermission")
    override fun start(onSamples: (ShortArray) -> Unit) {
        if (_isRunning) return

        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(1024)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            _isRunning = true

            captureJob = scope.launch {
                val buffer = ShortArray(512)
                while (isActive && _isRunning) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        onSamples(buffer)
                    }
                    // Limit processing cadence to ~20-25 Hz
                    delay(40)
                }
            }
        } catch (_: Exception) {
            stop()
        }
    }

    override fun stop() {
        _isRunning = false
        captureJob?.cancel()
        captureJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }
}

/**
 * Simulator audio source generating multi-tone frequencies for testing and offline visualizers.
 */
class SimulatedAudioSource(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    val sampleRate: Int = 44100,
    val toneHz: Double? = null
) : AudioSampleSource {

    private var simJob: Job? = null
    private var _isRunning = false
    override val isRunning: Boolean
        get() = _isRunning

    override fun start(onSamples: (ShortArray) -> Unit) {
        if (_isRunning) return
        _isRunning = true

        simJob = scope.launch {
            var phase = 0.0
            val bufferSize = 512
            val buffer = ShortArray(bufferSize)

            while (isActive && _isRunning) {
                for (i in 0 until bufferSize) {
                    if (toneHz != null) {
                        val s = sin(2.0 * PI * toneHz * (phase + i) / sampleRate) * 0.8
                        buffer[i] = (s * 32767.0).toInt().toShort()
                    } else {
                        // Combine bass (100 Hz), mid (800 Hz), and treble (4000 Hz)
                        val s1 = sin(2.0 * PI * 100.0 * (phase + i) / sampleRate) * 0.4
                        val s2 = sin(2.0 * PI * 800.0 * (phase + i) / sampleRate) * 0.3
                        val s3 = sin(2.0 * PI * 4000.0 * (phase + i) / sampleRate) * 0.2
                        buffer[i] = ((s1 + s2 + s3) * 32767.0).toInt().toShort()
                    }
                }
                phase += bufferSize
                onSamples(buffer)
                delay(50) // 20 Hz
            }
        }
    }

    override fun stop() {
        _isRunning = false
        simJob?.cancel()
        simJob = null
    }
}
