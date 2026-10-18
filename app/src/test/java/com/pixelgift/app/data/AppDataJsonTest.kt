package com.pixelgift.app.data

import com.pixelgift.app.data.model.AppData
import com.pixelgift.app.data.model.AppDataJson
import com.pixelgift.app.data.model.NamedPalette
import com.pixelgift.app.protocol.Rgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppDataJsonTest {

    @Test
    fun defaultAppData_roundTrip_isIdentical() {
        val original = AppData()
        val json = AppDataJson.toJson(original)
        val deserialized = AppDataJson.fromJson(json)

        assertEquals(original.version, deserialized.version)
        assertEquals(original.brightness, deserialized.brightness)
        assertEquals(original.favoriteColors, deserialized.favoriteColors)
        assertEquals(original.recentColors, deserialized.recentColors)
        assertEquals(original.palettes.size, deserialized.palettes.size)

        original.palettes.forEachIndexed { i, p ->
            val dp = deserialized.palettes[i]
            assertEquals(p.id, dp.id)
            assertEquals(p.name, dp.name)
            assertEquals(p.colors, dp.colors)
        }
    }

    @Test
    fun customAppData_roundTrip_isIdentical() {
        val custom = AppData(
            version = 2,
            brightness = 210,
            favoriteColors = listOf(
                Rgb(12, 34, 56),
                Rgb(78, 90, 120),
                Rgb(250, 200, 150)
            ),
            recentColors = listOf(
                Rgb(255, 0, 0),
                Rgb(0, 255, 0),
                Rgb(0, 0, 255)
            ),
            palettes = listOf(
                NamedPalette(
                    id = "custom_p1",
                    name = "Aurora Night",
                    colors = listOf(
                        Rgb(5, 240, 180),
                        Rgb(15, 120, 250),
                        Rgb(180, 20, 230)
                    )
                ),
                NamedPalette(
                    id = "custom_p2",
                    name = "Desert Fire",
                    colors = listOf(
                        Rgb(255, 80, 0),
                        Rgb(255, 200, 0)
                    )
                )
            )
        )

        val json = AppDataJson.toJson(custom)
        val imported = AppDataJson.fromJson(json)

        assertEquals(custom.version, imported.version)
        assertEquals(custom.brightness, imported.brightness)
        assertEquals(custom.favoriteColors, imported.favoriteColors)
        assertEquals(custom.recentColors, imported.recentColors)
        assertEquals(custom.palettes.size, imported.palettes.size)

        for (i in custom.palettes.indices) {
            assertEquals(custom.palettes[i].id, imported.palettes[i].id)
            assertEquals(custom.palettes[i].name, imported.palettes[i].name)
            assertEquals(custom.palettes[i].colors, imported.palettes[i].colors)
        }
    }
}
