package com.pixelgift.app.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Asserts that Kotlin constants in ProtocolConstants.kt strictly match
 * protocol/protocol.json. If either file changes without the other, this test FAILS.
 */
class ProtocolJsonConsistencyTest {

    private fun findProtocolJsonFile(): File {
        val candidates = listOf(
            File("../protocol/protocol.json"),
            File("protocol/protocol.json"),
            File("../../protocol/protocol.json"),
            File(System.getProperty("user.dir"), "../protocol/protocol.json")
        )
        for (f in candidates) {
            if (f.exists()) return f.canonicalFile
        }
        throw AssertionError("Could not find protocol/protocol.json in candidates: ${candidates.map { it.absolutePath }}")
    }

    @Test
    fun protocolJson_strictlyMatches_kotlinConstants() {
        val file = findProtocolJsonFile()
        assertTrue("protocol.json must exist", file.exists())

        val rawText = file.readText()
        val json = SimpleJsonParser.parse(rawText) as Map<*, *>

        // 1. Version
        val version = (json["version"] as Number).toInt()
        assertEquals(ProtocolConstants.PROTOCOL_VERSION, version)

        // 2. UUIDs
        val service = json["service"] as Map<*, *>
        assertEquals(ProtocolConstants.SERVICE_UUID.toString().lowercase(), (service["uuid"] as String).lowercase())

        val chars = json["characteristics"] as Map<*, *>
        val cmdChar = chars["command"] as Map<*, *>
        assertEquals(ProtocolConstants.COMMAND_UUID.toString().lowercase(), (cmdChar["uuid"] as String).lowercase())

        val statusChar = chars["status"] as Map<*, *>
        assertEquals(ProtocolConstants.STATUS_UUID.toString().lowercase(), (statusChar["uuid"] as String).lowercase())

        // 3. Notification headers
        val notifs = json["notifications"] as Map<*, *>
        assertEquals(ProtocolConstants.HEADER_ACK, (notifs["headerAck"] as Number).toInt())
        assertEquals(ProtocolConstants.HEADER_SNAPSHOT, (notifs["headerSnapshot"] as Number).toInt())

        // 4. Flags
        val flags = json["flags"] as Map<*, *>
        assertEquals(ProtocolConstants.FLAG_MUTED, (flags["FLAG_MUTED"] as Number).toInt())
        assertEquals(ProtocolConstants.FLAG_TIME_SYNCED, (flags["FLAG_TIME_SYNCED"] as Number).toInt())
        assertEquals(ProtocolConstants.FLAG_TIMER_RUNNING, (flags["FLAG_TIMER_RUNNING"] as Number).toInt())
        assertEquals(ProtocolConstants.FLAG_SHOW_RUNNING, (flags["FLAG_SHOW_RUNNING"] as Number).toInt())
        assertEquals(ProtocolConstants.FLAG_POWER_CAPPED, (flags["FLAG_POWER_CAPPED"] as Number).toInt())

        // 5. Error codes
        val errorCodes = json["errorCodes"] as Map<*, *>
        assertEquals(ProtocolConstants.ERROR_OK, (errorCodes["OK"] as Number).toInt())
        assertEquals(ProtocolConstants.ERROR_UNKNOWN_OP, (errorCodes["UNKNOWN_OP"] as Number).toInt())
        assertEquals(ProtocolConstants.ERROR_BAD_LEN, (errorCodes["BAD_LEN"] as Number).toInt())
        assertEquals(ProtocolConstants.ERROR_BAD_VALUE, (errorCodes["BAD_VALUE"] as Number).toInt())
        assertEquals(ProtocolConstants.ERROR_BUSY, (errorCodes["BUSY"] as Number).toInt())
        assertEquals(ProtocolConstants.ERROR_UNSUPPORTED, (errorCodes["UNSUPPORTED"] as Number).toInt())
        assertEquals(ProtocolConstants.ERROR_STORAGE_ERR, (errorCodes["STORAGE_ERR"] as Number).toInt())

        // 6. Opcodes (all 25 must match)
        val opcodes = json["opcodes"] as Map<*, *>
        val expectedOpcodes = mapOf(
            "SET_COLOR" to ProtocolConstants.OP_SET_COLOR,
            "SET_BRIGHTNESS" to ProtocolConstants.OP_SET_BRIGHTNESS,
            "SET_EFFECT" to ProtocolConstants.OP_SET_EFFECT,
            "SET_PALETTE" to ProtocolConstants.OP_SET_PALETTE,
            "SET_MODE" to ProtocolConstants.OP_SET_MODE,
            "SYNC_TIME" to ProtocolConstants.OP_SYNC_TIME,
            "SET_CLOCK_STYLE" to ProtocolConstants.OP_SET_CLOCK_STYLE,
            "START_TIMER" to ProtocolConstants.OP_START_TIMER,
            "CANCEL_TIMER" to ProtocolConstants.OP_CANCEL_TIMER,
            "PLAY_BIRTHDAY" to ProtocolConstants.OP_PLAY_BIRTHDAY,
            "STOP_SHOW" to ProtocolConstants.OP_STOP_SHOW,
            "SET_TEXT" to ProtocolConstants.OP_SET_TEXT,
            "SHOW_MESSAGE" to ProtocolConstants.OP_SHOW_MESSAGE,
            "SAVE_SCENE" to ProtocolConstants.OP_SAVE_SCENE,
            "LOAD_SCENE" to ProtocolConstants.OP_LOAD_SCENE,
            "SET_SCHEDULE" to ProtocolConstants.OP_SET_SCHEDULE,
            "SET_SLEEP_TIMER" to ProtocolConstants.OP_SET_SLEEP_TIMER,
            "DRAW_CHUNK" to ProtocolConstants.OP_DRAW_CHUNK,
            "DRAW_COMMIT" to ProtocolConstants.OP_DRAW_COMMIT,
            "MUSIC_LEVELS" to ProtocolConstants.OP_MUSIC_LEVELS,
            "SET_MUTE" to ProtocolConstants.OP_SET_MUTE,
            "GET_STATUS" to ProtocolConstants.OP_GET_STATUS,
            "CLEAR_BONDS" to ProtocolConstants.OP_CLEAR_BONDS,
            "FACTORY_RESET" to ProtocolConstants.OP_FACTORY_RESET,
            "OTA_BEGIN" to ProtocolConstants.OP_OTA_BEGIN
        )

        assertEquals("Exactly 25 opcodes defined", 25, expectedOpcodes.size)
        for ((name, expectedCode) in expectedOpcodes) {
            val opDef = opcodes[name] as? Map<*, *>
            assertNotNull("Opcode $name must exist in protocol.json", opDef)
            val code = (opDef!!["code"] as Number).toInt()
            assertEquals("Opcode value for $name", expectedCode, code)
        }

        // 7. Enums
        val enums = json["enums"] as Map<*, *>

        val modes = enums["mode"] as Map<*, *>
        assertEquals(ProtocolConstants.MODE_SOLID, (modes["SOLID"] as Number).toInt())
        assertEquals(ProtocolConstants.MODE_EFFECT, (modes["EFFECT"] as Number).toInt())
        assertEquals(ProtocolConstants.MODE_CLOCK, (modes["CLOCK"] as Number).toInt())
        assertEquals(ProtocolConstants.MODE_TIMER, (modes["TIMER"] as Number).toInt())
        assertEquals(ProtocolConstants.MODE_BIRTHDAY, (modes["BIRTHDAY"] as Number).toInt())
        assertEquals(ProtocolConstants.MODE_MESSAGE, (modes["MESSAGE"] as Number).toInt())
        assertEquals(ProtocolConstants.MODE_DRAW, (modes["DRAW"] as Number).toInt())
        assertEquals(ProtocolConstants.MODE_MUSIC, (modes["MUSIC"] as Number).toInt())
        assertEquals(ProtocolConstants.MODE_OFF, (modes["OFF"] as Number).toInt())

        val directions = enums["direction"] as Map<*, *>
        assertEquals(ProtocolConstants.DIR_LEFT, (directions["LEFT"] as Number).toInt())
        assertEquals(ProtocolConstants.DIR_RIGHT, (directions["RIGHT"] as Number).toInt())
        assertEquals(ProtocolConstants.DIR_UP, (directions["UP"] as Number).toInt())
        assertEquals(ProtocolConstants.DIR_DOWN, (directions["DOWN"] as Number).toInt())
        assertEquals(ProtocolConstants.DIR_IN, (directions["IN"] as Number).toInt())
        assertEquals(ProtocolConstants.DIR_OUT, (directions["OUT"] as Number).toInt())

        val colorModes = enums["colorMode"] as Map<*, *>
        assertEquals(ProtocolConstants.COLOR_MODE_SINGLE, (colorModes["SINGLE"] as Number).toInt())
        assertEquals(ProtocolConstants.COLOR_MODE_DUAL, (colorModes["DUAL"] as Number).toInt())
        assertEquals(ProtocolConstants.COLOR_MODE_PALETTE, (colorModes["PALETTE"] as Number).toInt())
        assertEquals(ProtocolConstants.COLOR_MODE_RAINBOW, (colorModes["RAINBOW"] as Number).toInt())

        val scheduleActions = enums["scheduleAction"] as Map<*, *>
        assertEquals(ProtocolConstants.SCHEDULE_ACTION_ON, (scheduleActions["ON"] as Number).toInt())
        assertEquals(ProtocolConstants.SCHEDULE_ACTION_OFF, (scheduleActions["OFF"] as Number).toInt())
        assertEquals(ProtocolConstants.SCHEDULE_ACTION_LOAD_SCENE, (scheduleActions["LOAD_SCENE"] as Number).toInt())
        assertEquals(ProtocolConstants.SCHEDULE_ACTION_SUNRISE_ALARM, (scheduleActions["SUNRISE_ALARM"] as Number).toInt())

        val textTargets = enums["textTarget"] as Map<*, *>
        assertEquals(ProtocolConstants.TEXT_TARGET_MESSAGE, (textTargets["MESSAGE"] as Number).toInt())
        assertEquals(ProtocolConstants.TEXT_TARGET_BIRTHDAY_NAME, (textTargets["BIRTHDAY_NAME"] as Number).toInt())
        assertEquals(ProtocolConstants.TEXT_TARGET_BIRTHDAY_GREETING, (textTargets["BIRTHDAY_GREETING"] as Number).toInt())
    }
}

/**
 * Lightweight recursive-descent JSON parser with zero external dependencies.
 */
object SimpleJsonParser {
    fun parse(text: String): Any? {
        return Parser(text.trim()).parseValue()
    }

    private class Parser(private val text: String) {
        private var pos = 0

        fun parseValue(): Any? {
            skipWhitespace()
            if (pos >= text.length) return null
            return when (text[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> {
                    if (text.startsWith("true", pos)) { pos += 4; true }
                    else if (text.startsWith("false", pos)) { pos += 5; false }
                    else throw IllegalArgumentException("Invalid boolean")
                }
                'n' -> {
                    if (text.startsWith("null", pos)) { pos += 4; null }
                    else throw IllegalArgumentException("Invalid null")
                }
                else -> parseNumber()
            }
        }

        private fun skipWhitespace() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        private fun parseString(): String {
            pos++ // skip opening quote
            val sb = StringBuilder()
            while (pos < text.length) {
                val c = text[pos++]
                if (c == '"') return sb.toString()
                if (c == '\\' && pos < text.length) {
                    when (val next = text[pos++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = text.substring(pos, pos + 4)
                            pos += 4
                            sb.append(hex.toInt(16).toChar())
                        }
                        else -> sb.append(next)
                    }
                } else {
                    sb.append(c)
                }
            }
            throw IllegalArgumentException("Unterminated string")
        }

        private fun parseNumber(): Number {
            val start = pos
            if (text[pos] == '-') pos++
            while (pos < text.length && (text[pos].isDigit() || text[pos] == '.' || text[pos] == 'e' || text[pos] == 'E' || text[pos] == '+' || text[pos] == '-')) {
                pos++
            }
            val numStr = text.substring(start, pos)
            return if (numStr.contains('.') || numStr.contains('e') || numStr.contains('E')) {
                numStr.toDouble()
            } else {
                numStr.toLong()
            }
        }

        private fun parseArray(): List<Any?> {
            pos++ // skip '['
            val list = mutableListOf<Any?>()
            skipWhitespace()
            if (pos < text.length && text[pos] == ']') {
                pos++
                return list
            }
            while (pos < text.length) {
                list.add(parseValue())
                skipWhitespace()
                if (pos < text.length && text[pos] == ',') {
                    pos++
                } else if (pos < text.length && text[pos] == ']') {
                    pos++
                    break
                }
            }
            return list
        }

        private fun parseObject(): Map<String, Any?> {
            pos++ // skip '{'
            val map = linkedMapOf<String, Any?>()
            skipWhitespace()
            if (pos < text.length && text[pos] == '}') {
                pos++
                return map
            }
            while (pos < text.length) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                require(pos < text.length && text[pos] == ':') { "Expected ':' after key $key" }
                pos++ // skip ':'
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (pos < text.length && text[pos] == ',') {
                    pos++
                } else if (pos < text.length && text[pos] == '}') {
                    pos++
                    break
                }
            }
            return map
        }
    }
}
