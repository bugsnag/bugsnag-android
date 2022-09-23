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

    fun trimNullableStringValuesTo(maxStringLength: Int, map: MutableMap<String, Any?>): TrimMetrics {
        var stringCount = 0
        var charCount = 0
        for (entry in map.entries) {
            val value = entry.value
            if (value is String && value.length > maxStringLength) {
                entry.setValue(stringTrimmedTo(maxStringLength, value))
                charCount += value.length - maxStringLength
                stringCount++
            }
        }

        return TrimMetrics(stringCount, charCount)
    }

    fun trimStringValuesTo(maxStringLength: Int, map: MutableMap<String, Any>): TrimMetrics {
        var stringCount = 0
        var charCount = 0
        for (entry in map.entries) {
            val value = entry.value
            if (value is String && value.length > maxStringLength) {
                entry.setValue(stringTrimmedTo(maxStringLength, value))
                charCount += value.length - maxStringLength
                stringCount++
            }
        }

        return TrimMetrics(stringCount, charCount)
    }
}
