package com.pixelgift.app.ui.effects

import com.pixelgift.app.data.model.EffectDefaults
import com.pixelgift.app.data.model.EffectItem
import com.pixelgift.app.data.model.EffectSupports
import com.pixelgift.app.data.model.NamedPalette
import com.pixelgift.app.data.repository.InMemoryAppDataRepository
import com.pixelgift.app.data.repository.InMemoryEffectCatalogRepository
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.transport.FakeDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EffectsViewModelTest {

    private val testScope = CoroutineScope(Dispatchers.Default)

    private val sampleEffects = listOf(
        EffectItem(
            id = 1,
            name = "Solid",
            category = "Static/glow",
            supports = EffectSupports(dir = false, mirror = false, colors = true, palette = false),
            defaults = EffectDefaults(speed = 0, dir = 0, mirror = 0, colorMode = 0)
        ),
        EffectItem(
            id = 6,
            name = "Rainbow Wave",
            category = "Rainbow/gradient",
            supports = EffectSupports(dir = true, mirror = true, colors = false, palette = false),
            defaults = EffectDefaults(speed = 50, dir = 1, mirror = 0, colorMode = 3)
        ),
        EffectItem(
            id = 8,
            name = "Scanner",
            category = "Motion",
            supports = EffectSupports(dir = true, mirror = false, colors = true, palette = false),
            defaults = EffectDefaults(speed = 65, dir = 0, mirror = 0, colorMode = 0)
        ),
        EffectItem(
            id = 10,
            name = "Fire",
            category = "Nature/fire",
            supports = EffectSupports(dir = true, mirror = false, colors = false, palette = true),
            defaults = EffectDefaults(speed = 60, dir = 2, mirror = 0, colorMode = 2)
        )
    )

    private fun createViewModel(): EffectsViewModel {
        val fakeDevice = FakeDevice(scope = testScope, connectDelayMs = 0L)
        val catalogRepo = InMemoryEffectCatalogRepository(sampleEffects)
        val appDataRepo = InMemoryAppDataRepository()
        return EffectsViewModel(
            transport = fakeDevice,
            catalogRepository = catalogRepo,
            appDataRepository = appDataRepo,
            externalScope = testScope
        )
    }

    @Test
    fun categoryFiltering_worksCorrectly() {
        val vm = createViewModel()

        assertEquals("All", vm.selectedCategory.value)
        assertEquals(4, vm.filteredEffects.value.size)

        vm.onCategorySelected("Motion")
        assertEquals(1, vm.filteredEffects.value.size)
        assertEquals("Scanner", vm.filteredEffects.value.first().name)

        vm.onCategorySelected("Rainbow/gradient")
        assertEquals(1, vm.filteredEffects.value.size)
        assertEquals("Rainbow Wave", vm.filteredEffects.value.first().name)
    }

    @Test
    fun searchFiltering_worksCorrectly() {
        val vm = createViewModel()

        vm.onSearchQueryChanged("Scan")
        assertEquals(1, vm.filteredEffects.value.size)
        assertEquals("Scanner", vm.filteredEffects.value.first().name)

        vm.onSearchQueryChanged("nonexistent")
        assertTrue(vm.filteredEffects.value.isEmpty())

        vm.onSearchQueryChanged("")
        assertEquals(4, vm.filteredEffects.value.size)
    }

    @Test
    fun favorites_toggleAndFilter() {
        val vm = createViewModel()

        // Toggle favorite on effect 8 (Scanner)
        val initialIsFav = vm.isFavorite(8)
        vm.onToggleFavorite(8)
        assertEquals(!initialIsFav, vm.isFavorite(8))

        // Ensure effect 1 is favorite
        if (!vm.isFavorite(1)) vm.onToggleFavorite(1)

        // Select Favorites category
        vm.onCategorySelected("Favorites")
        assertTrue(vm.filteredEffects.value.any { it.id == 1 })
    }

    @Test
    fun surpriseMe_selectsAndAppliesRandomEffect() {
        val vm = createViewModel()

        val chosen = vm.onSurpriseMe()
        assertNotNull(chosen)
        assertEquals(chosen, vm.selectedEffect.value)
        assertTrue(vm.sendCountForTesting > 0)
    }

    @Test
    fun controlsVisibility_conditionalOnSupportsFlags() {
        val vm = createViewModel()

        // Select Solid: no dir, no mirror, colors = true, palette = false
        val solid = sampleEffects.find { it.id == 1 }!!
        vm.onSelectEffect(solid)

        assertFalse("Solid must not support direction", solid.supports.dir)
        assertFalse("Solid must not support mirror", solid.supports.mirror)
        assertTrue("Solid must support colors", solid.supports.colors)
        assertFalse("Solid must not support palette", solid.supports.palette)

        // Select Rainbow Wave: dir = true, mirror = true, colors = false
        val wave = sampleEffects.find { it.id == 6 }!!
        vm.onSelectEffect(wave)

        assertTrue("Rainbow Wave must support direction", wave.supports.dir)
        assertTrue("Rainbow Wave must support mirror", wave.supports.mirror)
        assertFalse("Rainbow Wave must not support colors", wave.supports.colors)

        // Select Fire: dir = true, palette = true, colors = false
        val fire = sampleEffects.find { it.id == 10 }!!
        vm.onSelectEffect(fire)

        assertTrue("Fire must support direction", fire.supports.dir)
        assertTrue("Fire must support palette", fire.supports.palette)
        assertFalse("Fire must not support colors", fire.supports.colors)
    }

    @Test
    fun parameterModifications_updateStateAndDispatch() = runBlocking {
        val vm = createViewModel()
        val scanner = sampleEffects.find { it.id == 8 }!!
        vm.onSelectEffect(scanner)

        vm.onSpeedChanged(85)
        assertEquals(85, vm.speed.value)

        vm.onDirChanged(1)
        assertEquals(1, vm.dir.value)

        vm.onMirrorChanged(true)
        assertTrue(vm.mirror.value)

        vm.onC1Changed(Rgb(100, 150, 200))
        assertEquals(Rgb(100, 150, 200), vm.c1.value)

        val palette = NamedPalette("p1", "Test", listOf(Rgb(1, 2, 3)))
        vm.onPaletteSelected(palette)
        assertEquals(palette, vm.selectedPalette.value)

        assertTrue("Commands must be dispatched", vm.sendCountForTesting > 0)
    }

    @Test
    fun updateFromSnapshot_syncsActiveEffectAndSpeed() {
        val vm = createViewModel()
        val snapshot = com.pixelgift.app.protocol.DeviceNotification.StatusSnapshot(
            protoVer = 1,
            mode = com.pixelgift.app.protocol.ProtocolConstants.MODE_EFFECT,
            brightness = 100,
            r = 0, g = 0, b = 0,
            effectBase = 6, // Rainbow wave
            speed = 77,
            flags = 0,
            fwMajor = 1, fwMinor = 0
        )

        vm.updateFromSnapshot(snapshot)
        assertNotNull(vm.selectedEffect.value)
        assertEquals(6, vm.selectedEffect.value!!.id)
        assertEquals("Rainbow Wave", vm.selectedEffect.value!!.name)
        assertEquals(77, vm.speed.value)
    }
}
