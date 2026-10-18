package com.pixelgift.app.data.model

import com.pixelgift.app.protocol.Rgb
import com.pixelgift.app.ui.color.ColorMath

/**
 * Lightweight, zero-dependency pure Kotlin JSON parser and serializer
 * for AppData (palettes, favorites, settings) per SPEC 9.5.
 */
object AppDataJson {

    fun toJson(data: AppData, indent: Boolean = true): String {
        val sb = StringBuilder()
        val nl = if (indent) "\n" else ""
        val sp2 = if (indent) "  " else ""
        val sp4 = if (indent) "    " else ""
        val sp6 = if (indent) "      " else ""

        sb.append("{").append(nl)
        sb.append(sp2).append("\"version\": ").append(data.version).append(",").append(nl)
        sb.append(sp2).append("\"brightness\": ").append(data.brightness).append(",").append(nl)

        // favoriteColors
        sb.append(sp2).append("\"favoriteColors\": [")
        data.favoriteColors.forEachIndexed { i, c ->
            sb.append("\"").append(ColorMath.rgbToHex(c)).append("\"")
            if (i < data.favoriteColors.size - 1) sb.append(", ")
        }
        sb.append("],").append(nl)

        // recentColors
        sb.append(sp2).append("\"recentColors\": [")
        data.recentColors.forEachIndexed { i, c ->
            sb.append("\"").append(ColorMath.rgbToHex(c)).append("\"")
            if (i < data.recentColors.size - 1) sb.append(", ")
        }
        sb.append("],").append(nl)

        // palettes
        sb.append(sp2).append("\"palettes\": [").append(nl)
        data.palettes.forEachIndexed { pIdx, p ->
            sb.append(sp4).append("{").append(nl)
            sb.append(sp6).append("\"id\": \"").append(escapeJson(p.id)).append("\",").append(nl)
            sb.append(sp6).append("\"name\": \"").append(escapeJson(p.name)).append("\",").append(nl)
            sb.append(sp6).append("\"colors\": [")
            p.colors.forEachIndexed { cIdx, c ->
                sb.append("\"").append(ColorMath.rgbToHex(c)).append("\"")
                if (cIdx < p.colors.size - 1) sb.append(", ")
            }
            sb.append("]").append(nl)
            sb.append(sp4).append("}")
            if (pIdx < data.palettes.size - 1) sb.append(",")
            sb.append(nl)
        }
        sb.append(sp2).append("]").append(nl)
        sb.append("}")
        return sb.toString()
    }

    fun fromJson(jsonStr: String): AppData {
        val root = JsonParser(jsonStr).parse() as? JsonObject
            ?: throw IllegalArgumentException("Invalid JSON: root must be an object")

        val version = (root.map["version"] as? JsonNumber)?.value?.toInt() ?: 1
        val brightness = (root.map["brightness"] as? JsonNumber)?.value?.toInt()?.coerceIn(0, 255) ?: 128

        val favs = (root.map["favoriteColors"] as? JsonArray)?.list?.mapNotNull { item ->
            when (item) {
                is JsonString -> ColorMath.hexToRgb(item.value)
                is JsonObject -> {
                    val r = (item.map["r"] as? JsonNumber)?.value?.toInt() ?: 0
                    val g = (item.map["g"] as? JsonNumber)?.value?.toInt() ?: 0
                    val b = (item.map["b"] as? JsonNumber)?.value?.toInt() ?: 0
                    Rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
                }
                else -> null
            }
        } ?: AppData.DEFAULT_FAVORITES

        val recents = (root.map["recentColors"] as? JsonArray)?.list?.mapNotNull { item ->
            when (item) {
                is JsonString -> ColorMath.hexToRgb(item.value)
                is JsonObject -> {
                    val r = (item.map["r"] as? JsonNumber)?.value?.toInt() ?: 0
                    val g = (item.map["g"] as? JsonNumber)?.value?.toInt() ?: 0
                    val b = (item.map["b"] as? JsonNumber)?.value?.toInt() ?: 0
                    Rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
                }
                else -> null
            }
        } ?: AppData.DEFAULT_RECENTS

        val palettes = (root.map["palettes"] as? JsonArray)?.list?.mapNotNull { pVal ->
            val pObj = pVal as? JsonObject ?: return@mapNotNull null
            val id = (pObj.map["id"] as? JsonString)?.value ?: "palette_${System.currentTimeMillis()}"
            val name = (pObj.map["name"] as? JsonString)?.value ?: "Custom Palette"
            val colors = (pObj.map["colors"] as? JsonArray)?.list?.mapNotNull { cVal ->
                when (cVal) {
                    is JsonString -> ColorMath.hexToRgb(cVal.value)
                    is JsonObject -> {
                        val r = (cVal.map["r"] as? JsonNumber)?.value?.toInt() ?: 0
                        val g = (cVal.map["g"] as? JsonNumber)?.value?.toInt() ?: 0
                        val b = (cVal.map["b"] as? JsonNumber)?.value?.toInt() ?: 0
                        Rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
                    }
                    else -> null
                }
            }?.take(8) ?: emptyList()

            if (colors.isNotEmpty()) {
                NamedPalette(id, name, colors)
            } else {
                null
            }
        } ?: AppData.DEFAULT_PALETTES

        return AppData(
            version = version,
            brightness = brightness,
            favoriteColors = favs,
            recentColors = recents,
            palettes = palettes
        )
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    // Mini AST
    sealed class JsonValue
    data class JsonObject(val map: Map<String, JsonValue>) : JsonValue()
    data class JsonArray(val list: List<JsonValue>) : JsonValue()
    data class JsonString(val value: String) : JsonValue()
    data class JsonNumber(val value: Double) : JsonValue()
    data class JsonBoolean(val value: Boolean) : JsonValue()
    data object JsonNull : JsonValue()

    class JsonParser(private val src: String) {
        private var pos = 0

        fun parse(): JsonValue {
            skipWhitespace()
            val v = parseValue()
            skipWhitespace()
            return v
        }

        private fun parseValue(): JsonValue {
            skipWhitespace()
            if (pos >= src.length) throw IllegalArgumentException("Unexpected EOF")
            return when (src[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                else -> parseNumber()
            }
        }

        private fun parseObject(): JsonObject {
            expect('{')
            val map = LinkedHashMap<String, JsonValue>()
            skipWhitespace()
            if (peek() == '}') {
                pos++
                return JsonObject(map)
            }
            while (pos < src.length) {
                skipWhitespace()
                val key = parseString().value
                skipWhitespace()
                expect(':')
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (peek() == ',') {
                    pos++
                } else if (peek() == '}') {
                    pos++
                    break
                } else {
                    throw IllegalArgumentException("Expected ',' or '}' at pos $pos")
                }
            }
            return JsonObject(map)
        }

        private fun parseArray(): JsonArray {
            expect('[')
            val list = ArrayList<JsonValue>()
            skipWhitespace()
            if (peek() == ']') {
                pos++
                return JsonArray(list)
            }
            while (pos < src.length) {
                val value = parseValue()
                list.add(value)
                skipWhitespace()
                if (peek() == ',') {
                    pos++
                } else if (peek() == ']') {
                    pos++
                    break
                } else {
                    throw IllegalArgumentException("Expected ',' or ']' at pos $pos")
                }
            }
            return JsonArray(list)
        }

        private fun parseString(): JsonString {
            expect('"')
            val sb = StringBuilder()
            while (pos < src.length) {
                val c = src[pos++]
                if (c == '"') {
                    return JsonString(sb.toString())
                } else if (c == '\\') {
                    if (pos >= src.length) throw IllegalArgumentException("Unexpected EOF in string escape")
                    when (val esc = src[pos++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = src.substring(pos, pos + 4)
                            pos += 4
                            sb.append(hex.toInt(16).toChar())
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(c)
                }
            }
            throw IllegalArgumentException("Unterminated string")
        }

        private fun parseNumber(): JsonNumber {
            val start = pos
            if (peek() == '-') pos++
            while (pos < src.length && (src[pos].isDigit() || src[pos] == '.' || src[pos] == 'e' || src[pos] == 'E' || src[pos] == '+' || src[pos] == '-')) {
                pos++
            }
            val numStr = src.substring(start, pos)
            val num = numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $numStr at pos $start")
            return JsonNumber(num)
        }

        private fun parseBoolean(): JsonBoolean {
            if (src.startsWith("true", pos)) {
                pos += 4
                return JsonBoolean(true)
            }
            if (src.startsWith("false", pos)) {
                pos += 5
                return JsonBoolean(false)
            }
            throw IllegalArgumentException("Invalid boolean at pos $pos")
        }

        private fun parseNull(): JsonNull {
            if (src.startsWith("null", pos)) {
                pos += 4
                return JsonNull
            }
            throw IllegalArgumentException("Invalid null at pos $pos")
        }

        private fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) {
                pos++
            }
        }

        private fun peek(): Char = if (pos < src.length) src[pos] else '\u0000'

        private fun expect(expected: Char) {
            skipWhitespace()
            if (pos >= src.length || src[pos] != expected) {
                throw IllegalArgumentException("Expected '$expected' at pos $pos, got '${peek()}'")
            }
            pos++
        }
    }
}
