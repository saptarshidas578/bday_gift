package com.pixelgift.app.data.model

import com.pixelgift.app.protocol.Rgb

/**
 * A saved 9x9 pixel drawing (81 RGB pixels = 243 bytes frame).
 */
data class PixelDrawing(
    val id: String,
    val name: String,
    val pixels: List<Rgb>
) {
    init {
        require(pixels.size == 81) { "Pixel drawing must contain exactly 81 pixels, got ${pixels.size}" }
    }

    fun toFrameBytes(): ByteArray {
        val bytes = ByteArray(243)
        pixels.forEachIndexed { i, rgb ->
            bytes[i * 3] = rgb.r.toByte()
            bytes[i * 3 + 1] = rgb.g.toByte()
            bytes[i * 3 + 2] = rgb.b.toByte()
        }
        return bytes
    }

    companion object {
        fun fromFrameBytes(id: String, name: String, bytes: ByteArray): PixelDrawing {
            require(bytes.size == 243) { "Frame must be 243 bytes, got ${bytes.size}" }
            val pixels = ArrayList<Rgb>(81)
            for (i in 0 until 81) {
                val r = bytes[i * 3].toInt() and 0xFF
                val g = bytes[i * 3 + 1].toInt() and 0xFF
                val b = bytes[i * 3 + 2].toInt() and 0xFF
                pixels.add(Rgb(r, g, b))
            }
            return PixelDrawing(id, name, pixels)
        }

        val BLANK_CANVAS: List<Rgb> = List(81) { Rgb.BLACK }

        val HEART_PRESET: PixelDrawing by lazy {
            val p = BLANK_CANVAS.toMutableList()
            val red = Rgb(255, 45, 85)
            val heartIndices = listOf(
                10, 11, 15, 16,
                18, 19, 20, 21, 23, 24, 25, 26,
                27, 28, 29, 30, 31, 32, 33, 34, 35,
                37, 38, 39, 40, 41, 42, 43,
                47, 48, 49, 50, 51,
                57, 58, 59,
                67
            )
            for (idx in heartIndices) {
                if (idx in 0..80) p[idx] = red
            }
            PixelDrawing("preset_heart", "Heart", p)
        }

        val SMILE_PRESET: PixelDrawing by lazy {
            val p = BLANK_CANVAS.toMutableList()
            val yellow = Rgb(255, 214, 0)
            val smileIndices = listOf(
                20, 24, // Eyes
                38, 42, // Cheeks
                46, 52,
                56, 60,
                66, 67, 68
            )
            for (idx in smileIndices) {
                if (idx in 0..80) p[idx] = yellow
            }
            PixelDrawing("preset_smile", "Smile", p)
        }
    }
}
