package com.pixelgift.app.data.model

import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.protocol.Rgb

/**
 * A saved pixel drawing (${ProtocolConstants.MATRIX_WIDTH}x${ProtocolConstants.MATRIX_HEIGHT} = ${ProtocolConstants.MATRIX_LED_COUNT} RGB pixels = ${ProtocolConstants.MATRIX_FRAME_BYTES} bytes frame).
 */
data class PixelDrawing(
    val id: String,
    val name: String,
    val pixels: List<Rgb>
) {
    init {
        require(pixels.size == ProtocolConstants.MATRIX_LED_COUNT) {
            "Pixel drawing must contain exactly ${ProtocolConstants.MATRIX_LED_COUNT} pixels, got ${pixels.size}"
        }
    }

    fun toFrameBytes(): ByteArray {
        val bytes = ByteArray(ProtocolConstants.MATRIX_FRAME_BYTES)
        pixels.forEachIndexed { i, rgb ->
            bytes[i * 3] = rgb.r.toByte()
            bytes[i * 3 + 1] = rgb.g.toByte()
            bytes[i * 3 + 2] = rgb.b.toByte()
        }
        return bytes
    }

    companion object {
        fun fromFrameBytes(id: String, name: String, bytes: ByteArray): PixelDrawing {
            require(bytes.size == ProtocolConstants.MATRIX_FRAME_BYTES) {
                "Frame must be ${ProtocolConstants.MATRIX_FRAME_BYTES} bytes, got ${bytes.size}"
            }
            val pixels = ArrayList<Rgb>(ProtocolConstants.MATRIX_LED_COUNT)
            for (i in 0 until ProtocolConstants.MATRIX_LED_COUNT) {
                val r = bytes[i * 3].toInt() and 0xFF
                val g = bytes[i * 3 + 1].toInt() and 0xFF
                val b = bytes[i * 3 + 2].toInt() and 0xFF
                pixels.add(Rgb(r, g, b))
            }
            return PixelDrawing(id, name, pixels)
        }

        val BLANK_CANVAS: List<Rgb> = List(ProtocolConstants.MATRIX_LED_COUNT) { Rgb.BLACK }

        val HEART_PRESET: PixelDrawing by lazy {
            val p = BLANK_CANVAS.toMutableList()
            val red = Rgb(255, 45, 85)
            val heartIndices = listOf(
                12, 13, 16, 17,
                21, 22, 23, 24, 25, 26, 27, 28,
                31, 32, 33, 34, 35, 36, 37, 38,
                41, 42, 43, 44, 45, 46, 47, 48,
                52, 53, 54, 55, 56, 57,
                63, 64, 65, 66,
                74, 75
            )
            for (idx in heartIndices) {
                if (idx in 0 until ProtocolConstants.MATRIX_LED_COUNT) p[idx] = red
            }
            PixelDrawing("preset_heart", "Heart", p)
        }

        val SMILE_PRESET: PixelDrawing by lazy {
            val p = BLANK_CANVAS.toMutableList()
            val yellow = Rgb(255, 214, 0)
            val smileIndices = listOf(
                22, 23, 26, 27, // Eyes
                61, 68,         // Smile corners
                72, 77,
                83, 84, 85, 86  // Smile bottom
            )
            for (idx in smileIndices) {
                if (idx in 0 until ProtocolConstants.MATRIX_LED_COUNT) p[idx] = yellow
            }
            PixelDrawing("preset_smile", "Smile", p)
        }
    }
}
