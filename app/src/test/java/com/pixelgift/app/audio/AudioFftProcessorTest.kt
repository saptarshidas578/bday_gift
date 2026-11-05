package com.pixelgift.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class AudioFftProcessorTest {

    @Test
    fun processSamples_silence_returnsAllZeroBands() {
        val processor = AudioFftProcessor(sampleRate = 44100, fftSize = 512)
        val silence = ShortArray(512) { 0 }

        val levels = processor.processSamples(silence)

        assertEquals(10, levels.size)
        for (i in 0 until 10) {
            assertEquals("Band $i should be 0 on silence", 0.toByte(), levels[i])
        }
    }

    @Test
    fun fftSize_nonPowerOf2_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            AudioFftProcessor(sampleRate = 44100, fftSize = 500)
        }
    }

    @Test
    fun processSamples_lowFrequencySineWave_peaksInLowBand() {
        // Band 0: 40 Hz - 100 Hz. Inject 70 Hz sine wave.
        val processor = AudioFftProcessor(sampleRate = 44100, fftSize = 512)
        val samples = ShortArray(512)
        val freq = 70.0
        for (i in samples.indices) {
            val t = i.toDouble() / 44100.0
            samples[i] = (sin(2.0 * PI * freq * t) * 30000.0).toInt().toShort()
        }

        val levels = processor.processSamples(samples)
        val band0 = levels[0].toInt() and 0xFF
        val band8 = levels[8].toInt() and 0xFF
        val band9 = levels[9].toInt() and 0xFF

        assertTrue("Band 0 level ($band0) should be active (> 20)", band0 > 20)
        assertTrue("High bands should be near zero on 70 Hz tone", band8 < band0 / 2 && band9 < band0 / 2)
    }

    @Test
    fun processSamples_midFrequencySineWave_peaksInMidBand() {
        // Band 4: 900 Hz - 1800 Hz. Inject 1400 Hz sine wave.
        val processor = AudioFftProcessor(sampleRate = 44100, fftSize = 512)
        val samples = ShortArray(512)
        val freq = 1400.0
        for (i in samples.indices) {
            val t = i.toDouble() / 44100.0
            samples[i] = (sin(2.0 * PI * freq * t) * 30000.0).toInt().toShort()
        }

        val levels = processor.processSamples(samples)
        val band4 = levels[4].toInt() and 0xFF
        val band0 = levels[0].toInt() and 0xFF

        assertTrue("Band 4 level ($band4) should be active (> 30)", band4 > 30)
        assertTrue("Band 0 ($band0) should be much lower than Band 4 ($band4)", band0 < band4)
    }

    @Test
    fun processSamples_clamping_doesNotOverflow255() {
        // Maximum volume square wave to generate high harmonic energy
        val processor = AudioFftProcessor(sampleRate = 44100, fftSize = 512)
        val samples = ShortArray(512) { i ->
            if (i % 2 == 0) Short.MAX_VALUE else Short.MIN_VALUE
        }

        val levels = processor.processSamples(samples)
        assertEquals(10, levels.size)
        for (i in 0 until 10) {
            val unsignedVal = levels[i].toInt() and 0xFF
            assertTrue("Band $i should be clamped within 0..255, got $unsignedVal", unsignedVal in 0..255)
        }
    }
}
