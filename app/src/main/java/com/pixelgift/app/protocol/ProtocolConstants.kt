package com.pixelgift.app.protocol

import java.util.UUID

object ProtocolConstants {
    const val PROTOCOL_VERSION: Int = 1

    // Matrix Dimensions & Layout (Single source of truth per Check K)
    const val MATRIX_WIDTH: Int = 10
    const val MATRIX_HEIGHT: Int = 10
    const val MATRIX_LED_COUNT: Int = MATRIX_WIDTH * MATRIX_HEIGHT // 100 LEDs
    const val MATRIX_FRAME_BYTES: Int = MATRIX_LED_COUNT * 3       // 300 bytes

    // BLE Service and Characteristic UUIDs
    val SERVICE_UUID: UUID = UUID.fromString("7a1c0001-5d3e-4b8a-9c21-8f6d2e4a1b00")
    val COMMAND_UUID: UUID = UUID.fromString("7a1c0002-5d3e-4b8a-9c21-8f6d2e4a1b00")
    val STATUS_UUID: UUID = UUID.fromString("7a1c0003-5d3e-4b8a-9c21-8f6d2e4a1b00")

    // Notification Headers
    const val HEADER_ACK: Int = 0xA0
    const val HEADER_SNAPSHOT: Int = 0xA1

    // Status Notification Snapshot Length
    const val SNAPSHOT_PACKET_LENGTH: Int = 12
    const val ACK_PACKET_LENGTH: Int = 3

    // Flag bits in Snapshot
    const val FLAG_MUTED: Int = 1
    const val FLAG_TIME_SYNCED: Int = 2
    const val FLAG_TIMER_RUNNING: Int = 4
    const val FLAG_SHOW_RUNNING: Int = 8
    const val FLAG_POWER_CAPPED: Int = 16

    // Error Codes
    const val ERROR_OK: Int = 0
    const val ERROR_UNKNOWN_OP: Int = 1
    const val ERROR_BAD_LEN: Int = 2
    const val ERROR_BAD_VALUE: Int = 3
    const val ERROR_BUSY: Int = 4
    const val ERROR_UNSUPPORTED: Int = 5
    const val ERROR_STORAGE_ERR: Int = 6

    // Opcodes
    const val OP_SET_COLOR: Int = 0x01
    const val OP_SET_BRIGHTNESS: Int = 0x02
    const val OP_SET_EFFECT: Int = 0x03
    const val OP_SET_PALETTE: Int = 0x04
    const val OP_SET_MODE: Int = 0x05
    const val OP_SYNC_TIME: Int = 0x06
    const val OP_SET_CLOCK_STYLE: Int = 0x07
    const val OP_START_TIMER: Int = 0x08
    const val OP_CANCEL_TIMER: Int = 0x09
    const val OP_PLAY_BIRTHDAY: Int = 0x0A
    const val OP_STOP_SHOW: Int = 0x0B
    const val OP_SET_TEXT: Int = 0x0C
    const val OP_SHOW_MESSAGE: Int = 0x0D
    const val OP_SAVE_SCENE: Int = 0x0E
    const val OP_LOAD_SCENE: Int = 0x0F
    const val OP_SET_SCHEDULE: Int = 0x10
    const val OP_SET_SLEEP_TIMER: Int = 0x11
    const val OP_DRAW_CHUNK: Int = 0x12
    const val OP_DRAW_COMMIT: Int = 0x13
    const val OP_MUSIC_LEVELS: Int = 0x14
    const val OP_SET_MUTE: Int = 0x15
    const val OP_GET_STATUS: Int = 0x16
    const val OP_CLEAR_BONDS: Int = 0x17
    const val OP_FACTORY_RESET: Int = 0x18
    const val OP_OTA_BEGIN: Int = 0x19

    // Enums: Mode
    const val MODE_SOLID: Int = 0
    const val MODE_EFFECT: Int = 1
    const val MODE_CLOCK: Int = 2
    const val MODE_TIMER: Int = 3
    const val MODE_BIRTHDAY: Int = 4
    const val MODE_MESSAGE: Int = 5
    const val MODE_DRAW: Int = 6
    const val MODE_MUSIC: Int = 7
    const val MODE_OFF: Int = 8

    // Enums: Direction
    const val DIR_LEFT: Int = 0
    const val DIR_RIGHT: Int = 1
    const val DIR_UP: Int = 2
    const val DIR_DOWN: Int = 3
    const val DIR_IN: Int = 4
    const val DIR_OUT: Int = 5

    // Enums: ColorMode
    const val COLOR_MODE_SINGLE: Int = 0
    const val COLOR_MODE_DUAL: Int = 1
    const val COLOR_MODE_PALETTE: Int = 2
    const val COLOR_MODE_RAINBOW: Int = 3

    // Enums: ScheduleAction
    const val SCHEDULE_ACTION_ON: Int = 0
    const val SCHEDULE_ACTION_OFF: Int = 1
    const val SCHEDULE_ACTION_LOAD_SCENE: Int = 2
    const val SCHEDULE_ACTION_SUNRISE_ALARM: Int = 3

    // Enums: TextTarget
    const val TEXT_TARGET_MESSAGE: Int = 0
    const val TEXT_TARGET_BIRTHDAY_NAME: Int = 1
    const val TEXT_TARGET_BIRTHDAY_GREETING: Int = 2

    // Factory Reset Magic
    val FACTORY_RESET_MAGIC: ByteArray = byteArrayOf(0xA5.toByte(), 0x5A.toByte())
}
