package com.pixelgift.app.protocol

/**
 * 24-bit RGB Color Model.
 */
data class Rgb(
    val r: Int,
    val g: Int,
    val b: Int
) {
    init {
        require(r in 0..255) { "Red must be 0..255, got $r" }
        require(g in 0..255) { "Green must be 0..255, got $g" }
        require(b in 0..255) { "Blue must be 0..255, got $b" }
    }

    fun toByteArray(): ByteArray = byteArrayOf(r.toByte(), g.toByte(), b.toByte())

    companion object {
        val BLACK = Rgb(0, 0, 0)
        val WHITE = Rgb(255, 255, 255)
    }
}

/**
 * Commands sent from App to Device (little-endian payload).
 */
sealed class Command(val opcode: Int) {
    data class SetColor(val r: Int, val g: Int, val b: Int) : Command(ProtocolConstants.OP_SET_COLOR) {
        constructor(color: Rgb) : this(color.r, color.g, color.b)
    }

    data class SetBrightness(val brightness: Int) : Command(ProtocolConstants.OP_SET_BRIGHTNESS)

    data class SetEffect(
        val base: Int,
        val speed: Int,
        val dir: Int,
        val mirror: Int,
        val colorMode: Int,
        val c1: Rgb,
        val c2: Rgb
    ) : Command(ProtocolConstants.OP_SET_EFFECT)

    data class SetPalette(val colors: List<Rgb>) : Command(ProtocolConstants.OP_SET_PALETTE) {
        init {
            require(colors.isNotEmpty() && colors.size <= 8) {
                "Palette count must be 1..8, got ${colors.size}"
            }
        }
    }

    data class SetMode(val mode: Int) : Command(ProtocolConstants.OP_SET_MODE)

    data class SyncTime(val epochSeconds: Long, val tzMinutes: Int) : Command(ProtocolConstants.OP_SYNC_TIME)

    data class SetClockStyle(val style: Int) : Command(ProtocolConstants.OP_SET_CLOCK_STYLE)

    data class StartTimer(val seconds: Long) : Command(ProtocolConstants.OP_START_TIMER)

    data object CancelTimer : Command(ProtocolConstants.OP_CANCEL_TIMER)

    data object PlayBirthday : Command(ProtocolConstants.OP_PLAY_BIRTHDAY)

    data object StopShow : Command(ProtocolConstants.OP_STOP_SHOW)

    data class SetText(
        val target: Int,
        val idx: Int,
        val total: Int,
        val text: String
    ) : Command(ProtocolConstants.OP_SET_TEXT) {
        init {
            require(text.toByteArray(Charsets.US_ASCII).size <= 14) {
                "Text chunk must be at most 14 ASCII bytes"
            }
        }
    }

    data class ShowMessage(val speed: Int) : Command(ProtocolConstants.OP_SHOW_MESSAGE)

    data class SaveScene(val slot: Int) : Command(ProtocolConstants.OP_SAVE_SCENE) {
        init {
            require(slot in 0..7) { "Scene slot must be 0..7, got $slot" }
        }
    }

    data class LoadScene(val slot: Int) : Command(ProtocolConstants.OP_LOAD_SCENE) {
        init {
            require(slot in 0..7) { "Scene slot must be 0..7, got $slot" }
        }
    }

    data class SetSchedule(
        val slot: Int,
        val enabled: Boolean,
        val dayMask: Int,
        val hour: Int,
        val minute: Int,
        val action: Int,
        val arg: Int
    ) : Command(ProtocolConstants.OP_SET_SCHEDULE)

    data class SetSleepTimer(val minutes: Int) : Command(ProtocolConstants.OP_SET_SLEEP_TIMER)

    data class DrawChunk(val offset: Int, val data: ByteArray) : Command(ProtocolConstants.OP_DRAW_CHUNK) {
        init {
            require(data.size <= 14) { "Draw chunk must be <= 14 bytes, got ${data.size}" }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as DrawChunk
            return offset == other.offset && data.contentEquals(other.data)
        }

        override fun hashCode(): Int = 31 * offset + data.contentHashCode()
    }

    data object DrawCommit : Command(ProtocolConstants.OP_DRAW_COMMIT)

    data class MusicLevels(val levels: ByteArray) : Command(ProtocolConstants.OP_MUSIC_LEVELS) {
        init {
            require(levels.size == 9) { "Music levels must have 9 bytes, got ${levels.size}" }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as MusicLevels
            return levels.contentEquals(other.levels)
        }

        override fun hashCode(): Int = levels.contentHashCode()
    }

    data class SetMute(val muted: Boolean) : Command(ProtocolConstants.OP_SET_MUTE)

    data object GetStatus : Command(ProtocolConstants.OP_GET_STATUS)

    data object ClearBonds : Command(ProtocolConstants.OP_CLEAR_BONDS)

    data object FactoryReset : Command(ProtocolConstants.OP_FACTORY_RESET)

    data object OtaBegin : Command(ProtocolConstants.OP_OTA_BEGIN)
}

/**
 * Notifications emitted by the device over GATT STATUS characteristic.
 */
sealed interface DeviceNotification {
    /**
     * ACK packet: [0xA0][seq][code]
     */
    data class Ack(
        val seq: Int,
        val errorCode: Int
    ) : DeviceNotification {
        val isOk: Boolean get() = errorCode == ProtocolConstants.ERROR_OK
    }

    /**
     * Snapshot packet: [0xA1][protoVer][mode][brightness][r][g][b][effectBase][speed][flags][fwMajor][fwMinor]
     */
    data class StatusSnapshot(
        val protoVer: Int,
        val mode: Int,
        val brightness: Int,
        val r: Int,
        val g: Int,
        val b: Int,
        val effectBase: Int,
        val speed: Int,
        val flags: Int,
        val fwMajor: Int,
        val fwMinor: Int
    ) : DeviceNotification {
        val isMuted: Boolean get() = (flags and ProtocolConstants.FLAG_MUTED) != 0
        val isTimeSynced: Boolean get() = (flags and ProtocolConstants.FLAG_TIME_SYNCED) != 0
        val isTimerRunning: Boolean get() = (flags and ProtocolConstants.FLAG_TIMER_RUNNING) != 0
        val isShowRunning: Boolean get() = (flags and ProtocolConstants.FLAG_SHOW_RUNNING) != 0
        val isPowerCapped: Boolean get() = (flags and ProtocolConstants.FLAG_POWER_CAPPED) != 0

        val color: Rgb get() = Rgb(r, g, b)
    }
}
