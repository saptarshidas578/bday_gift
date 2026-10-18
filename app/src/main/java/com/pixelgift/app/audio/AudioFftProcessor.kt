package com.pixelgift.app.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Kotlin 9-band audio FFT processor for MUSIC_LEVELS reactive visualizer per SPEC 7.4.
 * Splits incoming audio into 9 musical frequency bands clamped to 0..255 (one per matrix column).
 */
class AudioFftProcessor(
    val sampleRate: Int = 44100,
    val fftSize: Int = 512
) {
    init {
        require((fftSize and (fftSize - 1)) == 0) { "FFT size must be a power of 2, got $fftSize" }
    }

    // 9 frequency band boundary edges (in Hz)
    val bandEdgesHz = doubleArrayOf(
        40.0, 120.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 7000.0, 11000.0, 16000.0
    )

    /**
     * Processes PCM 16-bit audio samples (e.g. from AudioRecord) and computes 9 band levels (0..255).
     */
    fun processSamples(samples: ShortArray): ByteArray {
        val n = fftSize
        val real = DoubleArray(n)
        val imag = DoubleArray(n)

        val count = minOf(samples.size, n)
        // Apply Hann window to reduce spectral leakage
        for (i in 0 until count) {
            val window = 0.5 * (1.0 - cos(2.0 * PI * i / (n - 1)))
            real[i] = (samples[i] / 32768.0) * window
            imag[i] = 0.0
        }

        // Perform Cooley-Tukey Radix-2 FFT
        fft(real, imag)

        // Compute magnitudes for first n / 2 bins
        val halfN = n / 2
        val binWidth = sampleRate.toDouble() / n
        val magnitudes = DoubleArray(halfN)
        for (i in 0 until halfN) {
            magnitudes[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }

        return mapMagnitudesTo9Bands(magnitudes, binWidth)
    }

    /**
     * Maps FFT bin magnitudes into 9 frequency bands.
     */
    fun mapMagnitudesTo9Bands(magnitudes: DoubleArray, binWidth: Double): ByteArray {
        val result = ByteArray(9)

        for (band in 0 until 9) {
            val lowFreq = bandEdgesHz[band]
            val highFreq = bandEdgesHz[band + 1]

            val startBin = (lowFreq / binWidth).toInt().coerceIn(0, magnitudes.size - 1)
            val endBin = (highFreq / binWidth).toInt().coerceIn(startBin, magnitudes.size - 1)

            var sum = 0.0
            var count = 0
            for (bin in startBin..endBin) {
                sum += magnitudes[bin]
                count++
            }

            val avg = if (count > 0) sum / count else 0.0
            // Boost factor tailored across frequency spectrum (higher frequencies carry less energy)
            val boost = 1.0 + (band * 0.4)
            val scaled = (avg * 1400.0 * boost).toInt().coerceIn(0, 255)
            result[band] = scaled.toByte()
        }

        return result
    }

    /**
     * In-place Radix-2 Decimation-in-Time FFT.
     */
    private fun fft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR

                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        var l = 2
        while (l <= n) {
            val halfL = l shr 1
            val angle = -2.0 * PI / l
            val wStepR = cos(angle)
            val wStepI = sin(angle)

            var i = 0
            while (i < n) {
                var wR = 1.0
                var wI = 0.0
                for (k in 0 until halfL) {
                    val uR = real[i + k]
                    val uI = imag[i + k]

                    val vR = real[i + k + halfL] * wR - imag[i + k + halfL] * wI
                    val vI = real[i + k + halfL] * wI + imag[i + k + halfL] * wR

                    real[i + k] = uR + vR
                    imag[i + k] = uI + vI
                    real[i + k + halfL] = uR - vR
                    imag[i + k + halfL] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += l
            }
            l = l shl 1
        }
    }
}
