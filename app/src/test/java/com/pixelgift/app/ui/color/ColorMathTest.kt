package com.pixelgift.app.ui.color

import com.pixelgift.app.protocol.Rgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ColorMathTest {

    @Test
    fun hsvToRgb_primaryAndSecondaryColors() {
        // Red
        assertEquals(Rgb(255, 0, 0), ColorMath.hsvToRgb(0f, 1f, 1f))
        // Yellow
        assertEquals(Rgb(255, 255, 0), ColorMath.hsvToRgb(60f, 1f, 1f))
        // Green
        assertEquals(Rgb(0, 255, 0), ColorMath.hsvToRgb(120f, 1f, 1f))
        // Cyan
        assertEquals(Rgb(0, 255, 255), ColorMath.hsvToRgb(180f, 1f, 1f))
        // Blue
        assertEquals(Rgb(0, 0, 255), ColorMath.hsvToRgb(240f, 1f, 1f))
        // Magenta
        assertEquals(Rgb(255, 0, 255), ColorMath.hsvToRgb(300f, 1f, 1f))
    }

    @Test
    fun hsvToRgb_greyscaleAndExtremes() {
        // Black
        assertEquals(Rgb(0, 0, 0), ColorMath.hsvToRgb(0f, 0f, 0f))
        assertEquals(Rgb(0, 0, 0), ColorMath.hsvToRgb(180f, 1f, 0f))
        // White
        assertEquals(Rgb(255, 255, 255), ColorMath.hsvToRgb(0f, 0f, 1f))
        assertEquals(Rgb(255, 255, 255), ColorMath.hsvToRgb(200f, 0f, 1f))
        // 50% Gray
        val gray = ColorMath.hsvToRgb(0f, 0f, 0.5f)
        assertEquals(128, gray.r)
        assertEquals(128, gray.g)
        assertEquals(128, gray.b)
    }

    @Test
    fun rgbToHsv_roundTripPrecision() {
        val testColors = listOf(
            Rgb(255, 0, 0),
            Rgb(0, 255, 0),
            Rgb(0, 0, 255),
            Rgb(255, 122, 0),   // Amber
            Rgb(0, 229, 255),   // Cyan
            Rgb(158, 119, 237), // Violet
            Rgb(255, 255, 255),
            Rgb(0, 0, 0)
        )

        for (original in testColors) {
            val hsv = ColorMath.rgbToHsv(original.r, original.g, original.b)
            val converted = ColorMath.hsvToRgb(hsv[0], hsv[1], hsv[2])

            assertTrue("R channel within rounding error for $original", abs(original.r - converted.r) <= 1)
            assertTrue("G channel within rounding error for $original", abs(original.g - converted.g) <= 1)
            assertTrue("B channel within rounding error for $original", abs(original.b - converted.b) <= 1)
        }
    }

    @Test
    fun hexFormatting_andParsing() {
        val amber = Rgb(255, 122, 0)
        val hex = ColorMath.rgbToHex(amber)
        assertEquals("#FF7A00", hex)

        val parsed = ColorMath.hexToRgb("#FF7A00")
        assertNotNull(parsed)
        assertEquals(amber, parsed)

        // Case insensitivity and missing hash
        val parsedNoHash = ColorMath.hexToRgb("ff7a00")
        assertEquals(amber, parsedNoHash)

        // Black and White
        assertEquals(Rgb(0, 0, 0), ColorMath.hexToRgb("#000000"))
        assertEquals(Rgb(255, 255, 255), ColorMath.hexToRgb("#FFFFFF"))
    }

    @Test
    fun hexParsing_invalidInputs_returnNull() {
        assertNull(ColorMath.hexToRgb(""))
        assertNull(ColorMath.hexToRgb("#FF"))
        assertNull(ColorMath.hexToRgb("#1234567"))
        assertNull(ColorMath.hexToRgb("#ZZZZZZ"))
        assertNull(ColorMath.hexToRgb("invalid"))
    }
}
