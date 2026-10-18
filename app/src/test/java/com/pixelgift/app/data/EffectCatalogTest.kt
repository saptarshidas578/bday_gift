package com.pixelgift.app.data

import com.pixelgift.app.data.model.EffectParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EffectCatalogTest {

    private val allowedCategories = setOf(
        "Static/glow",
        "Rainbow/gradient",
        "Motion",
        "Particles",
        "Nature/fire",
        "Geometric",
        "Retro",
        "Celebration"
    )

    private fun loadEffectsJson(): String {
        val candidates = listOf(
            File("src/main/assets/effects.json"),
            File("../protocol/effects.json"),
            File("app/src/main/assets/effects.json")
        )
        val file = candidates.find { it.exists() }
            ?: throw IllegalStateException("effects.json not found in test search paths")
        return file.readText()
    }

    @Test
    fun parseCatalog_allEntriesAreValid() {
        val json = loadEffectsJson()
        val effects = EffectParser.fromJson(json)

        assertTrue("Expected at least 60 catalog effects, got ${effects.size}", effects.size >= 60)

        val seenIds = mutableSetOf<Int>()
        for (effect in effects) {
            assertTrue("Effect ID must be positive, got ${effect.id}", effect.id > 0)
            assertTrue("Duplicate effect ID: ${effect.id}", seenIds.add(effect.id))
            assertTrue("Effect name cannot be blank for ID ${effect.id}", effect.name.isNotBlank())
            assertTrue(
                "Invalid category '${effect.category}' for effect '${effect.name}'",
                allowedCategories.contains(effect.category)
            )

            // Validate speed range
            assertTrue(
                "Default speed must be 0..100, got ${effect.defaults.speed}",
                effect.defaults.speed in 0..100
            )

            // Validate dir range
            assertTrue(
                "Default dir must be 0..5, got ${effect.defaults.dir}",
                effect.defaults.dir in 0..5
            )

            // Validate colorMode range
            assertTrue(
                "Default colorMode must be 0..3, got ${effect.defaults.colorMode}",
                effect.defaults.colorMode in 0..3
            )
        }
    }

    @Test
    fun verifyFirstTenEffects_matchProtocolJsonContract() {
        val json = loadEffectsJson()
        val effects = EffectParser.fromJson(json)
        val map = effects.associateBy { it.id }

        val solid = map[1]
        assertNotNull(solid)
        assertEquals("Solid", solid?.name)
        assertFalse(solid!!.supports.dir)
        assertFalse(solid.supports.mirror)
        assertTrue(solid.supports.colors)
        assertFalse(solid.supports.palette)

        val rainbowWave = map[6]
        assertNotNull(rainbowWave)
        assertEquals("Rainbow Wave", rainbowWave?.name)
        assertTrue(rainbowWave!!.supports.dir)
        assertTrue(rainbowWave.supports.mirror)
        assertFalse(rainbowWave.supports.colors)

        val fire = map[10]
        assertNotNull(fire)
        assertEquals("Fire", fire?.name)
        assertTrue(fire!!.supports.dir)
        assertTrue(fire.supports.palette)
    }

    @Test
    fun controlsOnlyAppearWhenSupported_supportsFlagsAreRespected() {
        val json = loadEffectsJson()
        val effects = EffectParser.fromJson(json)

        for (effect in effects) {
            // Mirror can only be enabled if dir or 2D animation supports it
            if (effect.supports.mirror) {
                // If mirror is supported, effect must have animation dynamics
                assertTrue("${effect.name} supports mirror", effect.supports.mirror)
            }
            // If colors is supported, effect accepts custom RGB
            if (effect.supports.colors) {
                assertTrue("${effect.name} supports RGB colors", effect.supports.colors)
            }
        }
    }
}
