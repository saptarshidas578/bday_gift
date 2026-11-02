package com.pixelgift.app.ui.color

import androidx.compose.ui.graphics.Color
import com.pixelgift.app.protocol.Rgb
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ColorMath {

    /**
     * Converts HSV values to an [Rgb] instance.
     * @param h Hue in degrees: [0f..360f)
     * @param s Saturation: [0f..1f]
     * @param v Value/Brightness: [0f..1f]
     */
    fun hsvToRgb(h: Float, s: Float, v: Float): Rgb {
        val clampedH = (h % 360f + 360f) % 360f
        val clampedS = s.coerceIn(0f, 1f)
        val clampedV = v.coerceIn(0f, 1f)

        val c = clampedV * clampedS
        val x = c * (1f - abs((clampedH / 60f) % 2f - 1f))
        val m = clampedV - c

        val (rPrime, gPrime, bPrime) = when ((clampedH / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((rPrime + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((gPrime + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((bPrime + m) * 255f).roundToInt().coerceIn(0, 255)

        return Rgb(r, g, b)
    }

    /**
     * Converts RGB integers (0..255) to HSV float array: [H: 0..360, S: 0..1, V: 0..1]
     */
    fun rgbToHsv(r: Int, g: Int, b: Int): FloatArray {
        val rNorm = r.coerceIn(0, 255) / 255f
        val gNorm = g.coerceIn(0, 255) / 255f
        val bNorm = b.coerceIn(0, 255) / 255f

        val cMax = max(rNorm, max(gNorm, bNorm))
        val cMin = min(rNorm, min(gNorm, bNorm))
        val delta = cMax - cMin

        val h = when {
            delta == 0f -> 0f
            cMax == rNorm -> (60f * ((gNorm - bNorm) / delta) + 360f) % 360f
            cMax == gNorm -> (60f * ((bNorm - rNorm) / delta) + 120f) % 360f
            else -> (60f * ((rNorm - gNorm) / delta) + 240f) % 360f
        }

        val s = if (cMax == 0f) 0f else delta / cMax
        val v = cMax

        return floatArrayOf(h, s, v)
    }

    /**
     * Formats an [Rgb] to standard hex format: #RRGGBB
     */
    fun rgbToHex(rgb: Rgb): String {
        return String.format("#%02X%02X%02X", rgb.r, rgb.g, rgb.b)
    }

    /**
     * Parses a hex string (#RRGGBB, RRGGBB, #RGB, or RGB) to [Rgb], or returns null if invalid.
     */
    fun hexToRgb(hex: String): Rgb? {
        val sanitized = hex.trim().removePrefix("#")
        val fullHex = when (sanitized.length) {
            3 -> "${sanitized[0]}${sanitized[0]}${sanitized[1]}${sanitized[1]}${sanitized[2]}${sanitized[2]}"
            6 -> sanitized
            else -> return null
        }

        return try {
            val r = fullHex.substring(0, 2).toInt(16)
            val g = fullHex.substring(2, 4).toInt(16)
            val b = fullHex.substring(4, 6).toInt(16)
            Rgb(r, g, b)
        } catch (_: NumberFormatException) {
            null
        }
    }

    /**
     * Converts an [Rgb] instance to a Compose [Color].
     */
    fun rgbToComposeColor(rgb: Rgb): Color {
        return Color(rgb.r, rgb.g, rgb.b)
    }

    /**
     * Converts HSV directly to a Compose [Color].
     */
    fun hsvToComposeColor(h: Float, s: Float, v: Float): Color {
        val rgb = hsvToRgb(h, s, v)
        return rgbToComposeColor(rgb)
    }
}
