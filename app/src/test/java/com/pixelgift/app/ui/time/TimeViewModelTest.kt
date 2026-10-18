package com.pixelgift.app.ui.time

import com.pixelgift.app.data.model.ScheduleItem
import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.DeviceNotification
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.transport.FakeDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeViewModelTest {

    private val testScope = CoroutineScope(Dispatchers.Default)

    @Test
    fun initialState_mirroredFromFakeDevice() {
        val fakeDevice = FakeDevice(scope = testScope)
        val viewModel = TimeViewModel(fakeDevice, externalScope = testScope)

        // FakeDevice initial status: flags = 0 (not synced, timer not running, show not running)
        assertFalse("Device clock should be unsynced initially", viewModel.isTimeSynced.value)
        assertFalse("Clock mode should be off initially", viewModel.isClockMode.value)
        assertFalse("Timer should be stopped initially", viewModel.isTimerRunning.value)
        assertEquals(0, viewModel.clockStyle.value)
    }

    @Test
    fun clockModeToggle_andStyleSelection() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        fakeDevice.connect("FA:KE:00:11:22:33")
        val viewModel = TimeViewModel(fakeDevice, externalScope = testScope)

        // Turn on Clock Mode
        viewModel.onToggleClockMode(true)
        assertTrue(viewModel.isClockMode.value)
        delay(50)
        assertEquals(ProtocolConstants.MODE_CLOCK, fakeDevice.status.value.mode)

        // Select Scrolling 5x7 Style (1)
        viewModel.onSelectClockStyle(1)
        assertEquals(1, viewModel.clockStyle.value)
        delay(50)

        // Select Stacked 3x4 Style (0)
        viewModel.onSelectClockStyle(0)
        assertEquals(0, viewModel.clockStyle.value)
        delay(50)

        // Turn off Clock Mode
        viewModel.onToggleClockMode(false)
        assertFalse(viewModel.isClockMode.value)
        delay(50)
        assertEquals(ProtocolConstants.MODE_SOLID, fakeDevice.status.value.mode)
    }

    @Test
    fun syncTime_manualAndAuto() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        fakeDevice.connect("FA:KE:00:11:22:33")
        val viewModel = TimeViewModel(fakeDevice, externalScope = testScope)

        // Manual sync
        viewModel.onManualSyncTime()
        assertTrue(viewModel.isTimeSynced.value)
        assertNotNull(viewModel.lastSyncTimestamp.value)
        delay(50)

        // Verify FakeDevice received sync and sets FLAG_TIME_SYNCED
        assertTrue(fakeDevice.status.value.isTimeSynced)

        // Simulate incoming snapshot where device power-cycles and loses time
        val unSyncedSnapshot = fakeDevice.status.value.copy(
            flags = fakeDevice.status.value.flags and ProtocolConstants.FLAG_TIME_SYNCED.inv()
        )
        viewModel.updateFromSnapshot(unSyncedSnapshot)
        assertFalse("ViewModel should mirror un-synced clock state", viewModel.isTimeSynced.value)

        // Trigger autoSyncOnConnect
        val preCount = viewModel.sendCountForTesting
        viewModel.autoSyncOnConnect()
        delay(50)
        assertTrue(viewModel.sendCountForTesting > preCount)
        assertTrue(viewModel.isTimeSynced.value)
    }

    @Test
    fun timer_startCancel_andSnapshotMirroring() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        fakeDevice.connect("FA:KE:00:11:22:33")
        val viewModel = TimeViewModel(fakeDevice, externalScope = testScope)

        // Start 5-minute timer (300 s)
        viewModel.onStartTimer(300L)
        assertTrue(viewModel.isTimerRunning.value)
        assertEquals(300L, viewModel.timerTotalSeconds.value)
        assertEquals(300L, viewModel.timerRemainingSeconds.value)

        delay(50)
        // Verify fake device received START_TIMER and set FLAG_TIMER_RUNNING
        assertTrue(fakeDevice.status.value.isTimerRunning)

        // Cancel timer
        viewModel.onCancelTimer()
        assertFalse(viewModel.isTimerRunning.value)
        delay(50)
        assertFalse(fakeDevice.status.value.isTimerRunning)

        // Simulate incoming snapshot where timer starts on device
        val runningSnapshot = fakeDevice.status.value.copy(
            flags = fakeDevice.status.value.flags or ProtocolConstants.FLAG_TIMER_RUNNING
        )
        viewModel.updateFromSnapshot(runningSnapshot)
        assertTrue("ViewModel should mirror timer running from device snapshot", viewModel.isTimerRunning.value)

        // Simulate incoming snapshot where timer stops on device
        val stoppedSnapshot = fakeDevice.status.value.copy(
            flags = fakeDevice.status.value.flags and ProtocolConstants.FLAG_TIMER_RUNNING.inv()
        )
        viewModel.updateFromSnapshot(stoppedSnapshot)
        assertFalse("ViewModel should mirror timer stopped from device snapshot", viewModel.isTimerRunning.value)
        assertEquals(0L, viewModel.timerRemainingSeconds.value)
    }

    @Test
    fun schedules_toggleAndSave() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        fakeDevice.connect("FA:KE:00:11:22:33")
        val viewModel = TimeViewModel(fakeDevice, externalScope = testScope)

        val initialSchedules = viewModel.schedules.value
        assertFalse(initialSchedules[0].enabled)

        // Toggle slot 0
        viewModel.onToggleSchedule(0)
        assertTrue(viewModel.schedules.value[0].enabled)
        delay(50)

        // Edit slot 0 to 08:15 AM
        val updated = viewModel.schedules.value[0].copy(hour = 8, minute = 15)
        viewModel.onSaveSchedule(updated)
        assertEquals(8, viewModel.schedules.value[0].hour)
        assertEquals(15, viewModel.schedules.value[0].minute)
    }

    @Test
    fun sleepTimer_setAndCancel() = runBlocking {
        val fakeDevice = FakeDevice(scope = testScope)
        fakeDevice.connect("FA:KE:00:11:22:33")
        val viewModel = TimeViewModel(fakeDevice, externalScope = testScope)

        // Set sleep timer to 30 min
        viewModel.onSetSleepTimer(30)
        assertEquals(30, viewModel.activeSleepMinutes.value)
        delay(50)

        // Cancel sleep timer
        viewModel.onCancelSleepTimer()
        assertNull(viewModel.activeSleepMinutes.value)
    }
}
