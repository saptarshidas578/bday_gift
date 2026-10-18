package com.pixelgift.app.ui.magic

import com.pixelgift.app.audio.SimulatedAudioSource
import com.pixelgift.app.data.model.PixelDrawing
import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.transport.FakeDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MagicViewModelTest {

    private val testScope = CoroutineScope(Dispatchers.Default)

    @Test
    fun birthday_playAndStop() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        val viewModel = MagicViewModel(fakeDevice, externalScope = testScope)

        viewModel.onPlayBirthday()
        delay(50)
        assertTrue(viewModel.isPlayingShow.value)
        assertEquals("Playing Birthday Show", viewModel.birthdayFeedback.value)

        viewModel.onStopShow()
        delay(50)
        assertFalse(viewModel.isPlayingShow.value)
        assertEquals("Show Stopped", viewModel.birthdayFeedback.value)
    }

    @Test
    fun birthday_saveContent_sendsChunks() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        val viewModel = MagicViewModel(fakeDevice, externalScope = testScope)

        val success = viewModel.onSaveBirthdayContent("Charlie", "Happy 25th!")
        assertTrue(success)
        assertEquals("Charlie", viewModel.birthdayName.value)
        assertEquals("Happy 25th!", viewModel.birthdayGreeting.value)

        delay(100)
        // Should have sent chunks to fake device
        assertTrue(viewModel.sendCountForTesting >= 2)

        // Invalid non-ascii rejected
        val fail = viewModel.onSaveBirthdayContent("Charlie \uD83C\uDF82", "Party")
        assertFalse(fail)
        assertTrue(viewModel.birthdayFeedback.value!!.contains("Error"))
    }

    @Test
    fun customMessage_validationAndSend() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        val viewModel = MagicViewModel(fakeDevice, externalScope = testScope)

        viewModel.onMessageTextChanged("ASCII ONLY 123")
        assertTrue(viewModel.isMessageAsciiValid.value)

        viewModel.onMessageTextChanged("Emojis are bad here: 🎉")
        assertFalse(viewModel.isMessageAsciiValid.value)

        // Cannot send invalid ASCII
        val sendFailed = viewModel.onSendMessage()
        assertFalse(sendFailed)

        // Reset to valid text and send
        viewModel.onMessageTextChanged("PixelGift Matrix")
        viewModel.onMessageSpeedChanged(75)
        val sendSuccess = viewModel.onSendMessage()
        assertTrue(sendSuccess)

        delay(100)
        assertTrue(viewModel.sendCountForTesting >= 2) // Chunks + ShowMessage
    }

    @Test
    fun drawCanvas_clickUndoClearCommitSave() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        val viewModel = MagicViewModel(fakeDevice, externalScope = testScope)

        val testColor = Rgb(0, 255, 128)
        viewModel.onSelectDrawColor(testColor)
        assertEquals(testColor, viewModel.selectedDrawColor.value)

        // Draw at pixel 40 (center of 9x9)
        viewModel.onPixelClicked(40)
        assertEquals(testColor, viewModel.canvasPixels.value[40])

        // Undo
        viewModel.onUndo()
        assertEquals(Rgb.BLACK, viewModel.canvasPixels.value[40])

        // Draw again and clear
        viewModel.onPixelClicked(0)
        assertEquals(testColor, viewModel.canvasPixels.value[0])
        viewModel.onClearCanvas()
        assertEquals(Rgb.BLACK, viewModel.canvasPixels.value[0])

        // Commit drawing (18 DRAW_CHUNK + 1 DRAW_COMMIT = 19 commands)
        val preCount = viewModel.sendCountForTesting
        viewModel.onCommitDrawing()
        delay(150)
        assertEquals(preCount + 19, viewModel.sendCountForTesting)

        // Save drawing
        val initialSavedCount = viewModel.savedDrawings.value.size
        viewModel.onSaveDrawing("My Rocket")
        assertEquals(initialSavedCount + 1, viewModel.savedDrawings.value.size)
        assertEquals("My Rocket", viewModel.savedDrawings.value[0].name)
    }

    @Test
    fun musicReactive_simulatedSourceUpdatesLevels() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        val viewModel = MagicViewModel(fakeDevice, externalScope = testScope)

        val simSource = SimulatedAudioSource(toneHz = 440.0)
        viewModel.startMusicReactive(simSource)
        assertTrue(viewModel.isMusicActive.value)

        // Wait for a few audio buffers to be processed
        delay(200)

        val levels = viewModel.liveLevels.value
        assertEquals(9, levels.size)
        // Simulated sine wave should generate energy
        assertTrue("Live levels should have non-zero energy", levels.any { it.toInt() != 0 })

        // Stop music visualizer
        viewModel.stopMusicReactive()
        assertFalse(viewModel.isMusicActive.value)
        assertEquals(0.toByte(), viewModel.liveLevels.value[0])
    }
}
