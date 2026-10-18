package com.pixelgift.app.ui.studio

import com.pixelgift.app.data.model.AppData
import com.pixelgift.app.data.model.NamedPalette
import com.pixelgift.app.data.repository.InMemoryAppDataRepository
import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.transport.FakeDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioViewModelTest {

    private val testScope = CoroutineScope(Dispatchers.Default)

    @Test
    fun initialColor_andTextInputs() {
        val fakeDevice = FakeDevice(scope = testScope)
        val viewModel = StudioViewModel(fakeDevice, externalScope = testScope)

        assertEquals(Rgb(255, 122, 0), viewModel.selectedColor.value)
        assertEquals("#FF7A00", viewModel.hexInput.value)
        assertEquals("255", viewModel.rInput.value)
        assertEquals("122", viewModel.gInput.value)
        assertEquals("0", viewModel.bInput.value)
    }

    @Test
    fun cancel_revertsToCommittedColor() {
        val fakeDevice = FakeDevice(scope = testScope)
        val viewModel = StudioViewModel(fakeDevice, externalScope = testScope)

        val original = viewModel.committedColor.value

        // Drag to a new color
        viewModel.onColorDragged(Rgb(0, 229, 255))
        assertEquals(Rgb(0, 229, 255), viewModel.selectedColor.value)

        // Cancel
        viewModel.onCancel()
        assertEquals(original, viewModel.selectedColor.value)
        assertEquals("#FF7A00", viewModel.hexInput.value)
    }

    @Test
    fun commitOk_updatesCommittedColor_andPrependsRecentColors() {
        val fakeDevice = FakeDevice(scope = testScope)
        val viewModel = StudioViewModel(fakeDevice, externalScope = testScope)

        val newColor = Rgb(158, 119, 237)
        viewModel.onColorDragged(newColor)
        viewModel.onCommitOk()

        assertEquals(newColor, viewModel.committedColor.value)
    }

    @Test
    fun dragging_isThrottled() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        val viewModel = StudioViewModel(fakeDevice, externalScope = testScope)

        // Rapidly simulate 20 drag events in under 50ms
        for (i in 0 until 20) {
            viewModel.onColorDragged(Rgb(i * 10, 100, 100))
        }

        assertTrue(
            "Rapid drag events must be throttled; expected sendCount <= 3, got ${viewModel.sendCountForTesting}",
            viewModel.sendCountForTesting <= 3
        )
    }

    @Test
    fun brightness_isThrottled_andUpdatesState() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        val repo = InMemoryAppDataRepository()
        val viewModel = StudioViewModel(fakeDevice, repository = repo, externalScope = testScope)

        viewModel.onBrightnessChanged(200)
        assertEquals(200, viewModel.brightness.value)

        // Rapidly simulate 20 slider moves
        for (b in 100..120) {
            viewModel.onBrightnessChanged(b)
        }

        assertTrue(
            "Rapid brightness slider events must be throttled; expected sendCount <= 3, got ${viewModel.brightnessSendCountForTesting}",
            viewModel.brightnessSendCountForTesting <= 3
        )
    }

    @Test
    fun favorites_management() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        val repo = InMemoryAppDataRepository()
        val viewModel = StudioViewModel(fakeDevice, repository = repo, externalScope = testScope)

        val testColor = Rgb(42, 84, 126)
        assertFalse(viewModel.isFavorite(testColor))

        viewModel.onAddFavorite(testColor)
        assertTrue(viewModel.isFavorite(testColor))
        assertTrue(viewModel.favoriteColors.value.contains(testColor))

        viewModel.onRemoveFavorite(testColor)
        assertFalse(viewModel.isFavorite(testColor))
        assertFalse(viewModel.favoriteColors.value.contains(testColor))
    }

    @Test
    fun scenes_saveAndLoad_dispatchesCommands() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        val repo = InMemoryAppDataRepository()
        val viewModel = StudioViewModel(fakeDevice, repository = repo, externalScope = testScope)

        viewModel.onLoadScene(3)
        assertEquals(3, viewModel.activeScene.value)
        assertEquals("Loaded Scene 4", viewModel.sceneFeedback.value)

        viewModel.onSaveScene(5)
        assertEquals(5, viewModel.activeScene.value)
        assertEquals("Saved Scene 6", viewModel.sceneFeedback.value)
    }

    @Test
    fun palettes_creation_andSelection() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        val repo = InMemoryAppDataRepository()
        val viewModel = StudioViewModel(fakeDevice, repository = repo, externalScope = testScope)

        val initialCount = viewModel.palettes.value.size
        viewModel.onCreatePalette("Test Palette", listOf(Rgb(10, 20, 30), Rgb(40, 50, 60)))

        assertEquals(initialCount + 1, viewModel.palettes.value.size)
        val created = viewModel.palettes.value.first()
        assertEquals("Test Palette", created.name)
        assertEquals(2, created.colors.size)

        // Select palette
        viewModel.onPaletteSelected(created)
        assertEquals(created, viewModel.selectedPalette.value)

        // Add color
        viewModel.onAddColorToPalette(created.id, Rgb(70, 80, 90))
        val updated = viewModel.palettes.value.first()
        assertEquals(3, updated.colors.size)

        // Rename
        viewModel.onRenamePalette(created.id, "Renamed Palette")
        assertEquals("Renamed Palette", viewModel.palettes.value.first().name)

        // Delete
        viewModel.onDeletePalette(created.id)
        assertEquals(initialCount, viewModel.palettes.value.size)
    }

    @Test
    fun exportAndImport_jsonRoundTrip_viaViewModel() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        val repo = InMemoryAppDataRepository()
        val viewModel = StudioViewModel(fakeDevice, repository = repo, externalScope = testScope)

        viewModel.onAddFavorite(Rgb(111, 222, 33))
        viewModel.onBrightnessChanged(185)

        val exportedJson = viewModel.exportJson()
        assertTrue(exportedJson.contains("\"brightness\": 185"))
        assertTrue(exportedJson.contains("#6FDE21"))

        // Create new clean repo and viewModel, import JSON
        val repo2 = InMemoryAppDataRepository()
        val viewModel2 = StudioViewModel(fakeDevice, repository = repo2, externalScope = testScope)

        val success = viewModel2.importJson(exportedJson)
        assertTrue(success)

        assertEquals(185, viewModel2.brightness.value)
        assertTrue(viewModel2.favoriteColors.value.contains(Rgb(111, 222, 33)))
    }
}
