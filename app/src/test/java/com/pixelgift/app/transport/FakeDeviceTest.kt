package com.pixelgift.app.transport

import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.DeviceNotification
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.protocol.Rgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeDeviceTest {

    @Test
    fun connect_transitionsStates_andEmitsInitialSnapshot() = runBlocking {
        val fakeDevice = FakeDevice(
            scope = this,
            connectDelayMs = 50L
        )

        assertEquals(ConnectionState.Disconnected, fakeDevice.connectionState.value)

        val receivedNotifs = mutableListOf<DeviceNotification>()
        val collectJob = launch {
            fakeDevice.notifications.collect { receivedNotifs.add(it) }
        }

        fakeDevice.connect("FA:KE:00:11:22:33")

        assertTrue(fakeDevice.connectionState.value is ConnectionState.Connected)
        val connectedState = fakeDevice.connectionState.value as ConnectionState.Connected
        assertEquals("PixelGift-Sim", connectedState.deviceName)
        assertEquals("FA:KE:00:11:22:33", connectedState.address)

        // Initial snapshot should have been emitted
        kotlinx.coroutines.yield()
        assertTrue("Notifications must not be empty", receivedNotifs.isNotEmpty())
        assertTrue(receivedNotifs[0] is DeviceNotification.StatusSnapshot)
        val snapshot = receivedNotifs[0] as DeviceNotification.StatusSnapshot
        assertEquals(ProtocolConstants.PROTOCOL_VERSION, snapshot.protoVer)

        collectJob.cancel()
    }

    @Test
    fun send_commands_emitsAck_andUpdatesSnapshot() = runBlocking {
        val fakeDevice = FakeDevice(
            scope = this,
            connectDelayMs = 10L
        )
        fakeDevice.connect()

        val notifs = mutableListOf<DeviceNotification>()
        val collectJob = launch {
            fakeDevice.notifications.collect { notifs.add(it) }
        }

        // Send SetColor
        val colorResult = fakeDevice.send(Command.SetColor(200, 100, 50))
        assertTrue(colorResult.isSuccess)

        // Verify status updated
        assertEquals(200, fakeDevice.status.value.r)
        assertEquals(100, fakeDevice.status.value.g)
        assertEquals(50, fakeDevice.status.value.b)
        assertEquals(ProtocolConstants.MODE_SOLID, fakeDevice.status.value.mode)

        // Send SetBrightness
        val brightResult = fakeDevice.send(Command.SetBrightness(180))
        assertTrue(brightResult.isSuccess)
        assertEquals(180, fakeDevice.status.value.brightness)

        // Send SetMute
        fakeDevice.send(Command.SetMute(true))
        assertTrue(fakeDevice.status.value.isMuted)

        fakeDevice.send(Command.SetMute(false))
        assertFalse(fakeDevice.status.value.isMuted)

        // Send SyncTime
        fakeDevice.send(Command.SyncTime(1700000000L, 0))
        assertTrue(fakeDevice.status.value.isTimeSynced)

        // Send StartTimer
        fakeDevice.send(Command.StartTimer(60L))
        assertTrue(fakeDevice.status.value.isTimerRunning)
        assertEquals(ProtocolConstants.MODE_TIMER, fakeDevice.status.value.mode)

        // Cancel Timer
        fakeDevice.send(Command.CancelTimer)
        assertFalse(fakeDevice.status.value.isTimerRunning)

        // Send PlayBirthday
        fakeDevice.send(Command.PlayBirthday)
        assertTrue(fakeDevice.status.value.isShowRunning)
        assertEquals(ProtocolConstants.MODE_BIRTHDAY, fakeDevice.status.value.mode)

        // Stop Show
        fakeDevice.send(Command.StopShow)
        assertFalse(fakeDevice.status.value.isShowRunning)

        // Verify ACKs were emitted
        kotlinx.coroutines.yield()
        val acks = notifs.filterIsInstance<DeviceNotification.Ack>()
        assertTrue("ACKs must be emitted for each command", acks.isNotEmpty())
        assertTrue("All ACKs must be OK", acks.all { it.isOk })

        collectJob.cancel()
    }

    @Test
    fun send_whenDisconnected_fails() = runBlocking {
        val fakeDevice = FakeDevice(scope = this)
        val result = fakeDevice.send(Command.SetColor(255, 0, 0))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun disconnect_transitionsToDisconnected() = runBlocking {
        val fakeDevice = FakeDevice(scope = this, connectDelayMs = 10L)
        fakeDevice.connect()
        assertTrue(fakeDevice.connectionState.value is ConnectionState.Connected)

        fakeDevice.disconnect()
        assertEquals(ConnectionState.Disconnected, fakeDevice.connectionState.value)
    }
}
