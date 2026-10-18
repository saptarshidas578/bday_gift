package com.pixelgift.app.protocol

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class MalformedPacketException(message: String) : Exception(message)

data class DecodedCommand(
    val seq: Int,
    val command: Command
)

object ProtocolCodec {

    /**
     * Encodes a command into a wire-format packet: [opcode u8][seq u8][payload...]
     */
    fun encodeCommand(command: Command, seq: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(command.opcode and 0xFF)
        out.write(seq and 0xFF)

        when (command) {
            is Command.SetColor -> {
                out.write(command.r and 0xFF)
                out.write(command.g and 0xFF)
                out.write(command.b and 0xFF)
            }
            is Command.SetBrightness -> {
                out.write(command.brightness and 0xFF)
            }
            is Command.SetEffect -> {
                out.write(command.base and 0xFF)
                out.write(command.speed and 0xFF)
                out.write(command.dir and 0xFF)
                out.write(command.mirror and 0xFF)
                out.write(command.colorMode and 0xFF)
                out.write(command.c1.r and 0xFF)
                out.write(command.c1.g and 0xFF)
                out.write(command.c1.b and 0xFF)
                out.write(command.c2.r and 0xFF)
                out.write(command.c2.g and 0xFF)
                out.write(command.c2.b and 0xFF)
            }
            is Command.SetPalette -> {
                out.write(command.colors.size and 0xFF)
                for (c in command.colors) {
                    out.write(c.r and 0xFF)
                    out.write(c.g and 0xFF)
                    out.write(c.b and 0xFF)
                }
            }
            is Command.SetMode -> {
                out.write(command.mode and 0xFF)
            }
            is Command.SyncTime -> {
                writeU32Le(out, command.epochSeconds)
                writeI16Le(out, command.tzMinutes)
            }
            is Command.SetClockStyle -> {
                out.write(command.style and 0xFF)
            }
            is Command.StartTimer -> {
                writeU32Le(out, command.seconds)
            }
            is Command.CancelTimer,
            is Command.PlayBirthday,
            is Command.StopShow -> {
                // No payload
            }
            is Command.SetText -> {
                out.write(command.target and 0xFF)
                out.write(command.idx and 0xFF)
                out.write(command.total and 0xFF)
                val textBytes = command.text.toByteArray(StandardCharsets.US_ASCII)
                out.write(textBytes)
            }
            is Command.ShowMessage -> {
                out.write(command.speed and 0xFF)
            }
            is Command.SaveScene -> {
                out.write(command.slot and 0xFF)
            }
            is Command.LoadScene -> {
                out.write(command.slot and 0xFF)
            }
            is Command.SetSchedule -> {
                out.write(command.slot and 0xFF)
                out.write(if (command.enabled) 1 else 0)
                out.write(command.dayMask and 0xFF)
                out.write(command.hour and 0xFF)
                out.write(command.minute and 0xFF)
                out.write(command.action and 0xFF)
                out.write(command.arg and 0xFF)
            }
            is Command.SetSleepTimer -> {
                writeU16Le(out, command.minutes)
            }
            is Command.DrawChunk -> {
                writeU16Le(out, command.offset)
                out.write(command.data)
            }
            is Command.DrawCommit -> {
                // No payload
            }
            is Command.MusicLevels -> {
                out.write(command.levels)
            }
            is Command.SetMute -> {
                out.write(if (command.muted) 1 else 0)
            }
            is Command.GetStatus,
            is Command.ClearBonds -> {
                // No payload
            }
            is Command.FactoryReset -> {
                out.write(ProtocolConstants.FACTORY_RESET_MAGIC)
            }
            is Command.OtaBegin -> {
                // No payload in v1 placeholder
            }
        }

        return out.toByteArray()
    }

    /**
     * Decodes a command packet [opcode u8][seq u8][payload...]
     */
    fun decodeCommand(bytes: ByteArray): DecodedCommand {
        if (bytes.size < 2) {
            throw MalformedPacketException("Command packet must be at least 2 bytes, got ${bytes.size}")
        }
        val opcode = bytes[0].toInt() and 0xFF
        val seq = bytes[1].toInt() and 0xFF
        val payload = bytes.copyOfRange(2, bytes.size)

        val cmd: Command = when (opcode) {
            ProtocolConstants.OP_SET_COLOR -> {
                if (payload.size != 3) throw MalformedPacketException("SET_COLOR payload must be 3 bytes, got ${payload.size}")
                Command.SetColor(
                    payload[0].toInt() and 0xFF,
                    payload[1].toInt() and 0xFF,
                    payload[2].toInt() and 0xFF
                )
            }
            ProtocolConstants.OP_SET_BRIGHTNESS -> {
                if (payload.size != 1) throw MalformedPacketException("SET_BRIGHTNESS payload must be 1 byte, got ${payload.size}")
                Command.SetBrightness(payload[0].toInt() and 0xFF)
            }
            ProtocolConstants.OP_SET_EFFECT -> {
                if (payload.size != 11) throw MalformedPacketException("SET_EFFECT payload must be 11 bytes, got ${payload.size}")
                Command.SetEffect(
                    base = payload[0].toInt() and 0xFF,
                    speed = payload[1].toInt() and 0xFF,
                    dir = payload[2].toInt() and 0xFF,
                    mirror = payload[3].toInt() and 0xFF,
                    colorMode = payload[4].toInt() and 0xFF,
                    c1 = Rgb(payload[5].toInt() and 0xFF, payload[6].toInt() and 0xFF, payload[7].toInt() and 0xFF),
                    c2 = Rgb(payload[8].toInt() and 0xFF, payload[9].toInt() and 0xFF, payload[10].toInt() and 0xFF)
                )
            }
            ProtocolConstants.OP_SET_PALETTE -> {
                if (payload.isEmpty()) throw MalformedPacketException("SET_PALETTE payload missing count")
                val count = payload[0].toInt() and 0xFF
                if (payload.size != 1 + count * 3) {
                    throw MalformedPacketException("SET_PALETTE length mismatch: count=$count requires ${1 + count * 3} bytes, got ${payload.size}")
                }
                val colors = ArrayList<Rgb>(count)
                var offset = 1
                for (i in 0 until count) {
                    colors.add(
                        Rgb(
                            payload[offset].toInt() and 0xFF,
                            payload[offset + 1].toInt() and 0xFF,
                            payload[offset + 2].toInt() and 0xFF
                        )
                    )
                    offset += 3
                }
                Command.SetPalette(colors)
            }
            ProtocolConstants.OP_SET_MODE -> {
                if (payload.size != 1) throw MalformedPacketException("SET_MODE payload must be 1 byte, got ${payload.size}")
                Command.SetMode(payload[0].toInt() and 0xFF)
            }
            ProtocolConstants.OP_SYNC_TIME -> {
                if (payload.size != 6) throw MalformedPacketException("SYNC_TIME payload must be 6 bytes, got ${payload.size}")
                val epoch = readU32Le(payload, 0)
                val tz = readI16Le(payload, 4)
                Command.SyncTime(epoch, tz)
            }
            ProtocolConstants.OP_SET_CLOCK_STYLE -> {
                if (payload.size != 1) throw MalformedPacketException("SET_CLOCK_STYLE payload must be 1 byte, got ${payload.size}")
                Command.SetClockStyle(payload[0].toInt() and 0xFF)
            }
            ProtocolConstants.OP_START_TIMER -> {
                if (payload.size != 4) throw MalformedPacketException("START_TIMER payload must be 4 bytes, got ${payload.size}")
                Command.StartTimer(readU32Le(payload, 0))
            }
            ProtocolConstants.OP_CANCEL_TIMER -> {
                if (payload.isNotEmpty()) throw MalformedPacketException("CANCEL_TIMER requires no payload")
                Command.CancelTimer
            }
            ProtocolConstants.OP_PLAY_BIRTHDAY -> {
                if (payload.isNotEmpty()) throw MalformedPacketException("PLAY_BIRTHDAY requires no payload")
                Command.PlayBirthday
            }
            ProtocolConstants.OP_STOP_SHOW -> {
                if (payload.isNotEmpty()) throw MalformedPacketException("STOP_SHOW requires no payload")
                Command.StopShow
            }
            ProtocolConstants.OP_SET_TEXT -> {
                if (payload.size < 3) throw MalformedPacketException("SET_TEXT payload must have at least target, idx, total")
                val target = payload[0].toInt() and 0xFF
                val idx = payload[1].toInt() and 0xFF
                val total = payload[2].toInt() and 0xFF
                val text = String(payload, 3, payload.size - 3, StandardCharsets.US_ASCII)
                Command.SetText(target, idx, total, text)
            }
            ProtocolConstants.OP_SHOW_MESSAGE -> {
                if (payload.size != 1) throw MalformedPacketException("SHOW_MESSAGE payload must be 1 byte, got ${payload.size}")
                Command.ShowMessage(payload[0].toInt() and 0xFF)
            }
            ProtocolConstants.OP_SAVE_SCENE -> {
                if (payload.size != 1) throw MalformedPacketException("SAVE_SCENE payload must be 1 byte, got ${payload.size}")
                Command.SaveScene(payload[0].toInt() and 0xFF)
            }
            ProtocolConstants.OP_LOAD_SCENE -> {
                if (payload.size != 1) throw MalformedPacketException("LOAD_SCENE payload must be 1 byte, got ${payload.size}")
                Command.LoadScene(payload[0].toInt() and 0xFF)
            }
            ProtocolConstants.OP_SET_SCHEDULE -> {
                if (payload.size != 7) throw MalformedPacketException("SET_SCHEDULE payload must be 7 bytes, got ${payload.size}")
                Command.SetSchedule(
                    slot = payload[0].toInt() and 0xFF,
                    enabled = (payload[1].toInt() and 0xFF) != 0,
                    dayMask = payload[2].toInt() and 0xFF,
                    hour = payload[3].toInt() and 0xFF,
                    minute = payload[4].toInt() and 0xFF,
                    action = payload[5].toInt() and 0xFF,
                    arg = payload[6].toInt() and 0xFF
                )
            }
            ProtocolConstants.OP_SET_SLEEP_TIMER -> {
                if (payload.size != 2) throw MalformedPacketException("SET_SLEEP_TIMER payload must be 2 bytes, got ${payload.size}")
                Command.SetSleepTimer(readU16Le(payload, 0))
            }
            ProtocolConstants.OP_DRAW_CHUNK -> {
                if (payload.size < 2) throw MalformedPacketException("DRAW_CHUNK payload must have at least 2 bytes offset")
                val offset = readU16Le(payload, 0)
                val data = payload.copyOfRange(2, payload.size)
                Command.DrawChunk(offset, data)
            }
            ProtocolConstants.OP_DRAW_COMMIT -> {
                if (payload.isNotEmpty()) throw MalformedPacketException("DRAW_COMMIT requires no payload")
                Command.DrawCommit
            }
            ProtocolConstants.OP_MUSIC_LEVELS -> {
                if (payload.size != 9) throw MalformedPacketException("MUSIC_LEVELS payload must be 9 bytes, got ${payload.size}")
                Command.MusicLevels(payload)
            }
            ProtocolConstants.OP_SET_MUTE -> {
                if (payload.size != 1) throw MalformedPacketException("SET_MUTE payload must be 1 byte, got ${payload.size}")
                Command.SetMute((payload[0].toInt() and 0xFF) != 0)
            }
            ProtocolConstants.OP_GET_STATUS -> {
                if (payload.isNotEmpty()) throw MalformedPacketException("GET_STATUS requires no payload")
                Command.GetStatus
            }
            ProtocolConstants.OP_CLEAR_BONDS -> {
                if (payload.isNotEmpty()) throw MalformedPacketException("CLEAR_BONDS requires no payload")
                Command.ClearBonds
            }
            ProtocolConstants.OP_FACTORY_RESET -> {
                if (payload.size != 2 || payload[0] != ProtocolConstants.FACTORY_RESET_MAGIC[0] || payload[1] != ProtocolConstants.FACTORY_RESET_MAGIC[1]) {
                    throw MalformedPacketException("FACTORY_RESET requires magic 0xA5 0x5A")
                }
                Command.FactoryReset
            }
            ProtocolConstants.OP_OTA_BEGIN -> {
                Command.OtaBegin
            }
            else -> throw MalformedPacketException("Unknown opcode: 0x${opcode.toString(16)}")
        }

        return DecodedCommand(seq, cmd)
    }

    /**
     * Encodes an ACK notification packet: [0xA0][seq][code]
     */
    fun encodeAck(seq: Int, errorCode: Int): ByteArray {
        return byteArrayOf(
            ProtocolConstants.HEADER_ACK.toByte(),
            (seq and 0xFF).toByte(),
            (errorCode and 0xFF).toByte()
        )
    }

    /**
     * Encodes a StatusSnapshot notification packet (12 bytes)
     */
    fun encodeSnapshot(snapshot: DeviceNotification.StatusSnapshot): ByteArray {
        return byteArrayOf(
            ProtocolConstants.HEADER_SNAPSHOT.toByte(),
            (snapshot.protoVer and 0xFF).toByte(),
            (snapshot.mode and 0xFF).toByte(),
            (snapshot.brightness and 0xFF).toByte(),
            (snapshot.r and 0xFF).toByte(),
            (snapshot.g and 0xFF).toByte(),
            (snapshot.b and 0xFF).toByte(),
            (snapshot.effectBase and 0xFF).toByte(),
            (snapshot.speed and 0xFF).toByte(),
            (snapshot.flags and 0xFF).toByte(),
            (snapshot.fwMajor and 0xFF).toByte(),
            (snapshot.fwMinor and 0xFF).toByte()
        )
    }

    /**
     * Decodes an incoming notification packet from the device STATUS characteristic.
     */
    fun decodeNotification(bytes: ByteArray): DeviceNotification {
        if (bytes.isEmpty()) {
            throw MalformedPacketException("Notification packet is empty")
        }

        val header = bytes[0].toInt() and 0xFF
        return when (header) {
            ProtocolConstants.HEADER_ACK -> {
                if (bytes.size != ProtocolConstants.ACK_PACKET_LENGTH) {
                    throw MalformedPacketException("ACK packet must be exactly 3 bytes, got ${bytes.size}")
                }
                val seq = bytes[1].toInt() and 0xFF
                val code = bytes[2].toInt() and 0xFF
                DeviceNotification.Ack(seq, code)
            }
            ProtocolConstants.HEADER_SNAPSHOT -> {
                if (bytes.size != ProtocolConstants.SNAPSHOT_PACKET_LENGTH) {
                    throw MalformedPacketException("Snapshot packet must be exactly 12 bytes, got ${bytes.size}")
                }
                DeviceNotification.StatusSnapshot(
                    protoVer = bytes[1].toInt() and 0xFF,
                    mode = bytes[2].toInt() and 0xFF,
                    brightness = bytes[3].toInt() and 0xFF,
                    r = bytes[4].toInt() and 0xFF,
                    g = bytes[5].toInt() and 0xFF,
                    b = bytes[6].toInt() and 0xFF,
                    effectBase = bytes[7].toInt() and 0xFF,
                    speed = bytes[8].toInt() and 0xFF,
                    flags = bytes[9].toInt() and 0xFF,
                    fwMajor = bytes[10].toInt() and 0xFF,
                    fwMinor = bytes[11].toInt() and 0xFF
                )
            }
            else -> throw MalformedPacketException("Unknown notification header: 0x${header.toString(16)}")
        }
    }

    // Little-endian binary helpers
    private fun writeU16Le(out: ByteArrayOutputStream, v: Int) {
        out.write(v and 0xFF)
        out.write((v ushr 8) and 0xFF)
    }

    private fun writeI16Le(out: ByteArrayOutputStream, v: Int) {
        out.write(v and 0xFF)
        out.write((v shr 8) and 0xFF)
    }

    private fun writeU32Le(out: ByteArrayOutputStream, v: Long) {
        out.write((v and 0xFF).toInt())
        out.write(((v ushr 8) and 0xFF).toInt())
        out.write(((v ushr 16) and 0xFF).toInt())
        out.write(((v ushr 24) and 0xFF).toInt())
    }

    private fun readU16Le(bytes: ByteArray, offset: Int): Int {
        val b0 = bytes[offset].toInt() and 0xFF
        val b1 = bytes[offset + 1].toInt() and 0xFF
        return (b1 shl 8) or b0
    }

    private fun readI16Le(bytes: ByteArray, offset: Int): Int {
        val b0 = bytes[offset].toInt() and 0xFF
        val b1 = bytes[offset + 1].toInt()
        return (b1 shl 8) or b0
    }

    private fun readU32Le(bytes: ByteArray, offset: Int): Long {
        val b0 = (bytes[offset].toLong() and 0xFF)
        val b1 = (bytes[offset + 1].toLong() and 0xFF) shl 8
        val b2 = (bytes[offset + 2].toLong() and 0xFF) shl 16
        val b3 = (bytes[offset + 3].toLong() and 0xFF) shl 24
        return b3 or b2 or b1 or b0
    }
}
