package com.pixelgift.app.ui.studio

import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.transport.FakeDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
        assertEquals(newColor, viewModel.recentColors.value.first())
    }

    @Test
    fun dragging_isThrottled() = runBlocking {
        val fakeDevice = FakeDevice(scope = this)
        val viewModel = StudioViewModel(fakeDevice, externalScope = this)

        // Rapidly simulate 20 drag events in under 50ms
        for (i in 0 until 20) {
            viewModel.onColorDragged(Rgb(i * 10, 100, 100))
        }

        // Due to 100ms throttle, immediate dispatch count is bounded to 1 (plus 1 trailing edge debounce)
        assertTrue(
            "Rapid drag events must be throttled; expected sendCount <= 3, got ${viewModel.sendCountForTesting}",
            viewModel.sendCountForTesting <= 3
        )
    }
}

