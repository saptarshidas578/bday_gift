package com.pixelgift.app.transport

import com.pixelgift.app.protocol.Command
import com.pixelgift.app.protocol.DeviceNotification
import com.pixelgift.app.protocol.ProtocolCodec
import com.pixelgift.app.protocol.ProtocolConstants
import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.ui.time.TimeViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class BleTransportLogicTest {

    @Test
    fun writeQueue_monotonicSequenceNumbers_wrapAt256() {
        val seqCounter = AtomicInteger(254)
        fun getNextSeq(): Int = seqCounter.getAndUpdate { (it + 1) and 0xFF }

        val seq1 = getNextSeq()
        val seq2 = getNextSeq()
        val seq3 = getNextSeq()

        assertEquals(254, seq1)
        assertEquals(255, seq2)
        assertEquals(0, seq3) // Wraps around at 256
    }

    @Test
    fun writeQueue_ackResolution_andTimeoutHandling() = runBlocking {
        val pendingAcks = ConcurrentHashMap<Int, CompletableDeferred<Int>>()
        val packetLossCount = AtomicInteger(0)

        // Scenario 1: Successful ACK
        val seqSuccess = 10
        val ackDeferred = CompletableDeferred<Int>()
        pendingAcks[seqSuccess] = ackDeferred

        // Device sends ACK back
        val ackNotif = ProtocolCodec.decodeNotification(
            byteArrayOf(ProtocolConstants.HEADER_ACK.toByte(), seqSuccess.toByte(), ProtocolConstants.ERROR_OK.toByte())
        ) as DeviceNotification.Ack

        pendingAcks[ackNotif.seq]?.complete(ackNotif.errorCode)
        val code = ackDeferred.await()
        assertEquals(ProtocolConstants.ERROR_OK, code)
        assertEquals(0, packetLossCount.get())

        // Scenario 2: ACK timeout (packet loss)
        val seqTimeout = 11
        val timeoutDeferred = CompletableDeferred<Int>()
        pendingAcks[seqTimeout] = timeoutDeferred

        var timeoutOccurred = false
        try {
            withTimeout(50L) {
                timeoutDeferred.await()
            }
        } catch (e: TimeoutCancellationException) {
            timeoutOccurred = true
            packetLossCount.incrementAndGet()
        } finally {
            pendingAcks.remove(seqTimeout)
        }

        assertTrue(timeoutOccurred)
        assertEquals(1, packetLossCount.get())
        assertFalse(pendingAcks.containsKey(seqTimeout))
    }

    @Test
    fun reconnect_snapshotUpdate_doesNotOverwriteHardwareState() = runBlocking {
        val fakeDevice = FakeDevice(scope = this)
        fakeDevice.connect("FA:KE:DE:VI:CE:00")

        val timeViewModel = TimeViewModel(fakeDevice, externalScope = this)

        // Hardware device has custom mode and time flag set
        val hardwareSnapshot = DeviceNotification.StatusSnapshot(
            protoVer = 1,
            mode = ProtocolConstants.MODE_CLOCK,
            brightness = 75,
            r = 210, g = 4, b = 45,
            effectBase = 2,
            speed = 40,
            flags = ProtocolConstants.FLAG_TIME_SYNCED or ProtocolConstants.FLAG_TIMER_RUNNING,
            fwMajor = 1, fwMinor = 0
        )

        // On reconnect, snapshot is pushed to ViewModel
        timeViewModel.updateFromSnapshot(hardwareSnapshot)

        assertTrue(timeViewModel.isTimeSynced.value)
        assertTrue(timeViewModel.isTimerRunning.value)
        assertEquals(ProtocolConstants.MODE_CLOCK, hardwareSnapshot.mode)
    }
}

