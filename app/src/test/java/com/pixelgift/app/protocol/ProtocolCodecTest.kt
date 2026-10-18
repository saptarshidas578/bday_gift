package com.pixelgift.app.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolCodecTest {

    @Test
    fun roundTrip_all25Opcodes() {
        val testCases: List<Pair<Command, Int>> = listOf(
            // 0x01 SET_COLOR
            Command.SetColor(255, 128, 64) to 1,
            // 0x02 SET_BRIGHTNESS
            Command.SetBrightness(120) to 2,
            // 0x03 SET_EFFECT
            Command.SetEffect(
                base = 6,
                speed = 45,
                dir = ProtocolConstants.DIR_RIGHT,
                mirror = 1,
                colorMode = ProtocolConstants.COLOR_MODE_RAINBOW,
                c1 = Rgb(255, 0, 0),
                c2 = Rgb(0, 255, 255)
            ) to 3,
            // 0x04 SET_PALETTE
            Command.SetPalette(
                listOf(
                    Rgb(255, 0, 0),
                    Rgb(0, 255, 0),
                    Rgb(0, 0, 255)
                )
            ) to 4,
            // 0x05 SET_MODE
            Command.SetMode(ProtocolConstants.MODE_EFFECT) to 5,
            // 0x06 SYNC_TIME
            Command.SyncTime(epochSeconds = 1770000000L, tzMinutes = 330) to 6,
            // 0x07 SET_CLOCK_STYLE
            Command.SetClockStyle(1) to 7,
            // 0x08 START_TIMER
            Command.StartTimer(seconds = 300L) to 8,
            // 0x09 CANCEL_TIMER
            Command.CancelTimer to 9,
            // 0x0A PLAY_BIRTHDAY
            Command.PlayBirthday to 10,
            // 0x0B STOP_SHOW
            Command.StopShow to 11,
            // 0x0C SET_TEXT
            Command.SetText(
                target = ProtocolConstants.TEXT_TARGET_BIRTHDAY_NAME,
                idx = 0,
                total = 1,
                text = "ALEX"
            ) to 12,
            // 0x0D SHOW_MESSAGE
            Command.ShowMessage(speed = 60) to 13,
            // 0x0E SAVE_SCENE
            Command.SaveScene(slot = 3) to 14,
            // 0x0F LOAD_SCENE
            Command.LoadScene(slot = 3) to 15,
            // 0x10 SET_SCHEDULE
            Command.SetSchedule(
                slot = 0,
                enabled = true,
                dayMask = 0x7F,
                hour = 8,
                minute = 30,
                action = ProtocolConstants.SCHEDULE_ACTION_ON,
                arg = 0
            ) to 16,
            // 0x11 SET_SLEEP_TIMER
            Command.SetSleepTimer(minutes = 45) to 17,
            // 0x12 DRAW_CHUNK
            Command.DrawChunk(
                offset = 18,
                data = byteArrayOf(10, 20, 30, 40, 50, 60)
            ) to 18,
            // 0x13 DRAW_COMMIT
            Command.DrawCommit to 19,
            // 0x14 MUSIC_LEVELS
            Command.MusicLevels(
                levels = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            ) to 20,
            // 0x15 SET_MUTE
            Command.SetMute(muted = true) to 21,
            // 0x16 GET_STATUS
            Command.GetStatus to 22,
            // 0x17 CLEAR_BONDS
            Command.ClearBonds to 23,
            // 0x18 FACTORY_RESET
            Command.FactoryReset to 24,
            // 0x19 OTA_BEGIN
            Command.OtaBegin to 25
        )

        assertEquals("Exactly 25 opcodes covered", 25, testCases.size)

        for ((originalCmd, seq) in testCases) {
            val encoded = ProtocolCodec.encodeCommand(originalCmd, seq)
            assertTrue("Encoded length must be at least 2 bytes", encoded.size >= 2)
            assertEquals("First byte must be opcode", originalCmd.opcode, encoded[0].toInt() and 0xFF)
            assertEquals("Second byte must be sequence number", seq, encoded[1].toInt() and 0xFF)

            val decoded = ProtocolCodec.decodeCommand(encoded)
            assertEquals("Decoded sequence number for opcode 0x${originalCmd.opcode.toString(16)}", seq, decoded.seq)
            assertEquals("Decoded command equals original for opcode 0x${originalCmd.opcode.toString(16)}", originalCmd, decoded.command)
        }
    }

    @Test
    fun notification_ack_roundTrip() {
        val bytes = ProtocolCodec.encodeAck(seq = 42, errorCode = ProtocolConstants.ERROR_OK)
        assertEquals(3, bytes.size)

        val notification = ProtocolCodec.decodeNotification(bytes)
        assertTrue(notification is DeviceNotification.Ack)
        val ack = notification as DeviceNotification.Ack
        assertEquals(42, ack.seq)
        assertEquals(ProtocolConstants.ERROR_OK, ack.errorCode)
        assertTrue(ack.isOk)

        // Error ACK
        val errBytes = ProtocolCodec.encodeAck(seq = 99, errorCode = ProtocolConstants.ERROR_BAD_LEN)
        val errAck = ProtocolCodec.decodeNotification(errBytes) as DeviceNotification.Ack
        assertEquals(99, errAck.seq)
        assertEquals(ProtocolConstants.ERROR_BAD_LEN, errAck.errorCode)
        assertFalse(errAck.isOk)
    }

    @Test
    fun notification_snapshot_roundTrip_andFlagHelpers() {
        val snapshot = DeviceNotification.StatusSnapshot(
            protoVer = 1,
            mode = ProtocolConstants.MODE_EFFECT,
            brightness = 150,
            r = 255,
            g = 120,
            b = 40,
            effectBase = 5,
            speed = 70,
            flags = ProtocolConstants.FLAG_TIME_SYNCED or ProtocolConstants.FLAG_SHOW_RUNNING,
            fwMajor = 1,
            fwMinor = 2
        )

        val bytes = ProtocolCodec.encodeSnapshot(snapshot)
        assertEquals(12, bytes.size)

        val decoded = ProtocolCodec.decodeNotification(bytes)
        assertTrue(decoded is DeviceNotification.StatusSnapshot)
        val s = decoded as DeviceNotification.StatusSnapshot
        assertEquals(snapshot, s)

        assertTrue("Time synced flag", s.isTimeSynced)
        assertTrue("Show running flag", s.isShowRunning)
        assertFalse("Muted flag", s.isMuted)
        assertFalse("Timer running flag", s.isTimerRunning)
        assertFalse("Power capped flag", s.isPowerCapped)
        assertEquals(Rgb(255, 120, 40), s.color)
    }

    @Test
    fun malformedCommandPackets_throwExpectedException() {
        // Less than 2 bytes
        assertThrows(MalformedPacketException::class.java) {
            ProtocolCodec.decodeCommand(byteArrayOf(0x01))
        }

        // Unknown opcode
        assertThrows(MalformedPacketException::class.java) {
            ProtocolCodec.decodeCommand(byteArrayOf(0xFF.toByte(), 0x01))
        }

        // Truncated SET_COLOR (needs 3 payload bytes)
        assertThrows(MalformedPacketException::class.java) {
            ProtocolCodec.decodeCommand(byteArrayOf(ProtocolConstants.OP_SET_COLOR.toByte(), 0x01, 10, 20))
        }

        // Truncated SET_EFFECT (needs 11 payload bytes)
        assertThrows(MalformedPacketException::class.java) {
            ProtocolCodec.decodeCommand(byteArrayOf(ProtocolConstants.OP_SET_EFFECT.toByte(), 0x01, 1, 2, 3))
        }

        // SET_PALETTE count mismatch
        assertThrows(MalformedPacketException::class.java) {
            ProtocolCodec.decodeCommand(
                byteArrayOf(
                    ProtocolConstants.OP_SET_PALETTE.toByte(),
                    0x01,
                    3, // expects 3 colors = 9 bytes
                    1, 2, 3 // only 1 color provided
                )
            )
        }

        // SYNC_TIME needs 6 bytes
        assertThrows(MalformedPacketException::class.java) {
            ProtocolCodec.decodeCommand(byteArrayOf(ProtocolConstants.OP_SYNC_TIME.toByte(), 0x01, 0, 0, 0))
        }

        // FACTORY_RESET with wrong magic
        assertThrows(MalformedPacketException::class.java) {
            ProtocolCodec.decodeCommand(byteArrayOf(ProtocolConstants.OP_FACTORY_RESET.toByte(), 0x01, 0x11, 0x22))
        }
    }

    @Test
    fun malformedNotificationPackets_throwExpectedException() {
        // Empty
        assertThrows(MalformedPacketException::class.java) {
            ProtocolCodec.decodeNotification(byteArrayOf())
        }

        // Unknown header
        assertThrows(MalformedPacketException::class.java) {
            ProtocolCodec.decodeNotification(byteArrayOf(0x55, 0x01, 0x02))
        }

        // Truncated ACK (less than 3 bytes)
        assertThrows(MalformedPacketException::class.java) {
            ProtocolCodec.decodeNotification(byteArrayOf(ProtocolConstants.HEADER_ACK.toByte(), 0x01))
        }

        // Truncated Snapshot (less than 12 bytes)
        assertThrows(MalformedPacketException::class.java) {
            ProtocolCodec.decodeNotification(byteArrayOf(ProtocolConstants.HEADER_SNAPSHOT.toByte(), 0x01, 0x02))
        }
    }
}
