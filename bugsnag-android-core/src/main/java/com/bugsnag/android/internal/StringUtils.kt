package com.bugsnag.android.internal

internal object StringUtils {
    fun stringTrimmedTo(maxLength: Int, str: String): String {
        val excessCharCount = str.length - maxLength
        return when {
            excessCharCount == 1 -> "${str.substring(0, maxLength)}***<1> CHAR TRUNCATED***"
            excessCharCount > 1 -> "${str.substring(0, maxLength)}***<$excessCharCount> CHARS TRUNCATED***"
            else -> str
        }
    }

    fun trimNullableStringValuesTo(maxStringLength: Int, map: MutableMap<String, Any?>): Pair<Int, Int> {
        var charCount = 0
        @Suppress("UNCHECKED_CAST")
        val toTrim = map.filterValues { it is String && it.length > maxStringLength } as Map<String, String>
        toTrim.forEach {
            charCount += it.value.length - maxStringLength
            map[it.key] = stringTrimmedTo(maxStringLength, it.value)
        }

        return Pair(toTrim.size, charCount)
    }

    fun trimStringValuesTo(maxStringLength: Int, map: MutableMap<String, Any>): Pair<Int, Int> {
        var charCount = 0
        @Suppress("UNCHECKED_CAST")
        val toTrim = map.filterValues { it is String && it.length > maxStringLength } as Map<String, String>
        toTrim.forEach {
            charCount += it.value.length - maxStringLength
            map[it.key] = stringTrimmedTo(maxStringLength, it.value)
        }

        return Pair(toTrim.size, charCount)
    }
}
