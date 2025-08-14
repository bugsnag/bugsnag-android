package com.bugsnag.android.internal

import java.io.InputStream
import kotlin.math.pow

/**
 * Fast and lightweight JSON parser that maps directly to Collection objects.
 * Uses `LinkedHashMap` for objects and `ArrayList` for arrays. Numbers are parsed as `Long` or
 * `Double` depending on their format. Directly decodes UTF-8 from `InputStream` to avoid the
 * overheads associated with `Charset` and `Reader` implementations, and as such as no
 * "charset" parameter.
 */
class JsonCollectionParser(private val input: InputStream) {
    private var current = -1
    private var position = 0L

    /**
     * Reusable StringBuilder for building strings, so that we avoid creating new
     * buffers every time we parse a string.
     */
    private val stringBuilder = StringBuilder()

    init {
        advance()
    }

    private fun advance(): Int {
        current = input.read()
        if (current != -1) position++
        return current
    }

    private fun readUtf8Char(): Int {
        if (current == -1) return -1

        val firstByte = current
        advance()

        // ASCII (0xxxxxxx)
        if (firstByte and 0x80 == 0) {
            return firstByte
        }

        // Multi-byte UTF-8 sequence
        val numBytes = when {
            firstByte and 0xE0 == 0xC0 -> 2 // 110xxxxx
            firstByte and 0xF0 == 0xE0 -> 3 // 1110xxxx
            firstByte and 0xF8 == 0xF0 -> 4 // 11110xxx
            else -> throw JsonParseException("Invalid UTF-8 sequence at position $position")
        }

        var codePoint = when (numBytes) {
            2 -> firstByte and 0x1F
            3 -> firstByte and 0x0F
            4 -> firstByte and 0x07
            else -> throw JsonParseException("Invalid UTF-8 sequence at position $position")
        }

        // Read continuation bytes (10xxxxxx)
        repeat(numBytes - 1) {
            if (current == -1 || current and 0xC0 != 0x80) {
                throw JsonParseException("Invalid UTF-8 continuation byte at position $position")
            }
            codePoint = (codePoint shl 6) or (current and 0x3F)
            advance()
        }

        // Validate code point ranges
        when (numBytes) {
            2 -> if (codePoint < 0x80) throw JsonParseException("Overlong UTF-8 encoding at position $position")
            3 -> if (codePoint < 0x800) throw JsonParseException("Overlong UTF-8 encoding at position $position")
            4 -> if (codePoint < 0x10000) throw JsonParseException("Overlong UTF-8 encoding at position $position")
        }

        // Check for invalid code points
        if (codePoint > 0x10FFFF || (codePoint >= 0xD800 && codePoint <= 0xDFFF)) {
            throw JsonParseException("Invalid Unicode code point at position $position")
        }

        return codePoint
    }

    private fun skipWhitespace() {
        while (current != -1) {
            if (current < CHAR_LOOKUP_TABLE_SIZE && WHITESPACE[current]) {
                advance()
            } else {
                break
            }
        }
    }

    private fun expect(expected: Int) {
        if (current != expected) {
            throw JsonParseException(
                "Expected '${expected.toChar()}' but got " +
                        "'${if (current == -1) "EOF" else current.toChar()}' at position $position"
            )
        }
        advance()
    }

    private fun parseString(): String {
        expect('"'.code)
        stringBuilder.clear()

        while (current != -1 && current != '"'.code) {
            if (current == '\\'.code) {
                advance()
                if (current == -1 || current >= CHAR_LOOKUP_TABLE_SIZE || !VALID_ESCAPES[current]) {
                    throw JsonParseException("Invalid escape sequence at position $position")
                }

                if (current == 'u'.code) {
                    advance()
                    val hex = CharArray(4) {
                        if (!isHexDigit(current)) {
                            throw JsonParseException("Invalid unicode escape at position $position")
                        }
                        current.toChar().also { advance() }
                    }
                    stringBuilder.append(String(hex).toInt(16).toChar())
                    continue
                } else {
                    stringBuilder.append(ESCAPE_CHARS[current])
                    advance()
                }
            } else {
                // Read UTF-8 character
                val codePoint = readUtf8Char()
                if (codePoint == -1) break

                // Convert code point to characters and append
                if (codePoint <= 0xFFFF) {
                    stringBuilder.append(codePoint.toChar())
                } else {
                    // Surrogate pair for code points > 0xFFFF
                    val high = ((codePoint - 0x10000) shr 10) + 0xD800
                    val low = ((codePoint - 0x10000) and 0x3FF) + 0xDC00
                    stringBuilder.append(high.toChar())
                    stringBuilder.append(low.toChar())
                }
            }
        }

        expect('"'.code)
        return stringBuilder.toString()
    }

    private fun parseNumber(): Number {
        // Store the result type
        var isDouble = false
        var parsedLong: Long
        var parsedDouble = 0.0

        // Track if the number is negative
        val isNegative = current == '-'.code
        if (isNegative) {
            advance()
        }

        // Parse as an integer until we find something that isn't a digit
        parsedLong = 0L
        if (current == '0'.code) {
            advance()
        } else if (isDigit(current)) {
            // Read first digit
            parsedLong = (current - '0'.code).toLong()
            advance()

            // Read remaining digits
            while (isDigit(current)) {
                // Check for overflow
                if (parsedLong > Long.MAX_VALUE / 10) {
                    // Switch to double for large numbers
                    parsedDouble = parsedLong.toDouble()
                    isDouble = true

                    // Continue parsing as double
                    while (isDigit(current)) {
                        parsedDouble = parsedDouble * 10 + (current - '0'.code)
                        advance()
                    }
                    break
                }

                // Multiply by 10 and add the next digit
                parsedLong = parsedLong * 10 + (current - '0'.code)
                advance()
            }
        } else {
            throw JsonParseException("Invalid number at position $position")
        }

        // Apply sign for long values
        if (isNegative && isDouble) {
            parsedLong = -parsedLong
        }

        // Is this a double? if so: continue parsing as a double
        if (current == '.'.code) {
            // Switch to double
            if (!isDouble) {
                parsedDouble = parsedLong.toDouble()
                isDouble = true
            }

            advance()
            if (!isDigit(current)) {
                throw JsonParseException("Invalid number at position $position")
            }

            var fraction = 0.0
            var fractionMultiplier = 0.1

            while (isDigit(current)) {
                fraction += (current - '0'.code) * fractionMultiplier
                fractionMultiplier *= 0.1
                advance()
            }

            parsedDouble += fraction
        }

        // do we have an exponent?
        if (current == 'e'.code || current == 'E'.code) {
            // Switch to double
            if (!isDouble) {
                parsedDouble = parsedLong.toDouble()
                isDouble = true
            }

            advance()

            // Handle exponent sign
            val exponentIsNegative = current == '-'.code
            if (current == '+'.code || current == '-'.code) {
                advance()
            }

            if (!isDigit(current)) {
                throw JsonParseException("Invalid number at position $position")
            }

            // Parse exponent value
            var exponent = 0
            while (isDigit(current)) {
                exponent = exponent * 10 + (current - '0'.code)
                advance()
            }

            // Apply exponent
            parsedDouble *= 10.0.pow(
                if (exponentIsNegative) -exponent.toDouble()
                else exponent.toDouble()
            )
        }

        // Apply sign for double values
        if (isNegative && isDouble) {
            parsedDouble = -parsedDouble
        }

        // Return the appropriate type
        return if (!isDouble) {
            parsedLong
        } else {
            parsedDouble
        }
    }

    private fun parseKeyword(keyword: String): Any? {
        keyword.forEach { expectedChar ->
            if (current != expectedChar.code) {
                throw JsonParseException("Expected '$keyword' at position $position")
            }
            advance()
        }
        return when (keyword) {
            "true" -> true
            "false" -> false
            "null" -> null
            else -> throw JsonParseException("Unknown keyword '$keyword'")
        }
    }

    private fun parseArray(): ArrayList<Any?> {
        expect('['.code)
        skipWhitespace()

        val list = ArrayList<Any?>()

        if (current == ']'.code) {
            advance()
            return list
        }

        while (true) {
            list.add(parseValue())
            skipWhitespace()

            when (current) {
                ','.code -> {
                    advance()
                    skipWhitespace()
                }

                ']'.code -> {
                    advance()
                    break
                }

                else -> throw JsonParseException("Expected ',' or ']' at position $position")
            }
        }

        return list
    }

    private fun parseObject(): LinkedHashMap<String, Any?> {
        expect('{'.code)
        skipWhitespace()

        val map = LinkedHashMap<String, Any?>()

        if (current == '}'.code) {
            advance()
            return map
        }

        while (true) {
            if (current != '"'.code) {
                throw JsonParseException("Expected string key at position $position")
            }

            val key = parseString()
            skipWhitespace()
            expect(':'.code)
            skipWhitespace()
            val value = parseValue()

            map[key] = value
            skipWhitespace()

            when (current) {
                ','.code -> {
                    advance()
                    skipWhitespace()
                }

                '}'.code -> {
                    advance()
                    break
                }

                else -> throw JsonParseException("Expected ',' or '}' at position $position")
            }
        }

        return map
    }

    private fun parseValue(): Any? {
        skipWhitespace()

        return when (current) {
            '"'.code -> parseString()
            '{'.code -> parseObject()
            '['.code -> parseArray()
            't'.code -> parseKeyword("true")
            'f'.code -> parseKeyword("false")
            'n'.code -> parseKeyword("null")
            '-'.code, in '0'.code..'9'.code -> parseNumber()
            else -> {
                val char = if (current == -1) "EOF" else {
                    // For error reporting, try to decode the UTF-8 character
                    val tempCurrent = current
                    val codePoint = readUtf8Char()
                    current = tempCurrent // Reset for proper error handling
                    if (codePoint != -1 && codePoint <= 0xFFFF) {
                        codePoint.toChar().toString()
                    } else {
                        "U+${codePoint.toString(16).uppercase()}"
                    }
                }
                throw JsonParseException("Unexpected character '$char' at position $position")
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun isDigit(c: Int): Boolean = c in '0'.code..'9'.code

    @Suppress("NOTHING_TO_INLINE")
    private inline fun isHexDigit(c: Int): Boolean =
        c in '0'.code..'9'.code || c in 'a'.code..'f'.code || c in 'A'.code..'F'.code

    fun parse(): Any? {
        val result = parseValue()
        skipWhitespace()
        if (current != -1) {
            throw JsonParseException("Unexpected content after JSON at position $position")
        }
        return result
    }

    internal companion object {
        /**
         * Our lookup table size is 128 covering all ASCII characters.
         * This allows us to quickly check for whitespace and valid escape characters.
         */
        const val CHAR_LOOKUP_TABLE_SIZE = 128

        // Lookup table for whitespace characters (faster than multiple comparisons)
        @JvmStatic
        val WHITESPACE = lookupTable {
            this[0x20] = true // space
            this[0x09] = true // tab
            this[0x0A] = true // newline
            this[0x0D] = true // carriage return
        }

        // Lookup table to identify valid escape characters
        @JvmStatic
        val VALID_ESCAPES = lookupTable {
            this['"'.code] = true
            this['\\'.code] = true
            this['/'.code] = true
            this['b'.code] = true
            this['f'.code] = true
            this['n'.code] = true
            this['r'.code] = true
            this['t'.code] = true
            this['u'.code] = true
        }

        // Lookup table for escape sequences
        @JvmStatic
        val ESCAPE_CHARS = CharArray(CHAR_LOOKUP_TABLE_SIZE).apply {
            this['"'.code] = '"'
            this['\\'.code] = '\\'
            this['/'.code] = '/'
            this['b'.code] = '\b'
            this['f'.code] = '\u000C'
            this['n'.code] = '\n'
            this['r'.code] = '\r'
            this['t'.code] = '\t'
        }

        internal inline fun lookupTable(init: BooleanArray.() -> Unit): BooleanArray {
            return BooleanArray(CHAR_LOOKUP_TABLE_SIZE).apply(init)
        }
    }

    class JsonParseException(message: String) : Exception(message)
}
