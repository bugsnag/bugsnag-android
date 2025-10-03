package com.bugsnag.android

import com.bugsnag.android.internal.json.JsonCollectionPath

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Extracts data from JSON structures represented as collections objects (such as those returned
 * by [com.bugsnag.android.internal.JsonCollectionParser]). These are used when matching locally
 * stored events against remotely configured [DiscardRule]s.
 */
internal sealed class JsonDataExtractor(val path: String) : JsonStream.Streamable {
    private lateinit var _jsonPath: JsonCollectionPath
    protected val jsonPath: JsonCollectionPath
        get() {
            if (!this::_jsonPath.isInitialized) {
                _jsonPath = JsonCollectionPath.Companion.fromString(path)
            }

            return _jsonPath
        }

    /**
     * Run this data extractor on the JSON object stored in [root] and send the result to [output].
     * [output] may be called any number of times depending on the content of [root] and the
     * rules defined in the specific extractor implementation.
     */
    abstract fun extract(root: Map<String, *>, output: (String) -> Unit)

    fun extract(root: Map<String, *>): List<String> {
        val out = ArrayList<String>()
        extract(root, out::add)
        return out
    }

    protected fun stringify(item: Any): String? {
        return when (item) {
            is String -> item
            is Short, is Int, is Long, is Float, is Double, is Boolean -> item.toString()
            else -> null
        }
    }

    companion object {
        fun fromJsonMap(json: Map<String, *>): JsonDataExtractor? {
            val path = json["path"] as? String ?: return null
            val pathMode = json["pathMode"] as? String

            return when (pathMode) {
                "REGEX" -> regexExtractorFromMap(path, json)
                "FILTER" -> filterExtractorForMap(path, json)
                "RELATIVE_ADDRESS" -> RelativeAddressExtractor(path)
                null -> SimplePathExtractor(path)
                else -> null
            }
        }

        /**
         * Shorthand for `json.mapNotNull(this::fromJsonMap)`.
         */
        fun fromJsonList(json: List<Map<String, *>>): List<JsonDataExtractor> {
            return json.mapNotNull(this::fromJsonMap)
        }

        private fun filterExtractorForMap(path: String, json: Map<String, *>): JsonDataExtractor? {
            val filterPath = json["filterPath"] as? String ?: return null
            val matchTypeString = json["matchType"] as? String ?: return null
            val matchType = try {
                FilterExtractor.MatchType.valueOf(matchTypeString)
            } catch (_: IllegalArgumentException) {
                return null
            }
            val expectedValue = json["value"] as? String

            @Suppress("UNCHECKED_CAST")
            val subPathJson = json["subPath"] as? Map<String, *>
            val subExtractor = fromJsonMap(subPathJson as Map<String, *>) ?: return null

            return FilterExtractor(path, filterPath, matchType, expectedValue, subExtractor)
        }

        private fun regexExtractorFromMap(path: String, json: Map<String, *>): JsonDataExtractor? {
            val regex = json["regex"] as? String ?: return null
            return RegexExtractor(path, regex)
        }
    }
}

internal class SimplePathExtractor(path: String) : JsonDataExtractor(path) {
    override fun extract(root: Map<String, *>, output: (String) -> Unit) {
        jsonPath.extractFrom(root).forEach { item ->
            stringify(item)?.let { output(it) }
        }
    }

    override fun toStream(stream: JsonStream) {
        stream.beginObject()
        stream.name("path").value(path)
        stream.endObject()
    }

    override fun toString(): String {
        return path
    }
}

internal class RegexExtractor(path: String, regex: String) : JsonDataExtractor(path) {
    private val regex: Pattern = regex.toPattern()

    override fun extract(
        root: Map<String, *>,
        output: (String) -> Unit
    ) {
        for (item in jsonPath.extractFrom(root)) {
            val stringValue = stringify(item)
                ?: continue
            val matcher = regex.matcher(stringValue)

            // we *find* any match within the source string rather than require a full match
            if (matcher.find()) {
                output(extractGroups(stringValue, matcher))
            }
        }
    }

    private fun extractGroups(source: String, matcher: Matcher): String {
        if (matcher.groupCount() > 0) {
            // the regex groups are joined together with a ',' separator
            val groups = buildString(source.length) {
                matcher.group(1)?.let { append(it) }
                for (groupIndex in 2..matcher.groupCount()) {
                    append(',')
                    matcher.group(groupIndex)?.let { append(it) }
                }
            }
            return groups
        }

        return source
    }

    override fun toStream(stream: JsonStream) {
        stream.beginObject()
        stream.name("path").value(path)
        stream.name("pathMode").value("REGEX")
        stream.name("regex").value(regex.toString())
        stream.endObject()
    }

    override fun toString(): String {
        return "$path[$regex]"
    }
}

internal class FilterExtractor(
    path: String,
    filterPath: String,
    val matchType: MatchType,
    val expectedValue: String?,
    val subExtractor: JsonDataExtractor
) : JsonDataExtractor(path) {
    private val jsonFilterPath: JsonCollectionPath =
        JsonCollectionPath.Companion.fromString(filterPath)

    override fun extract(root: Map<String, *>, output: (String) -> Unit) {
        jsonPath.extractFrom(root)
            .filterIsInstance<Map<String, *>>()
            .filter { subRoot ->
                // the filterPath must point to exactly one value
                // does the stringified value match the expected value?
                val value = jsonFilterPath.extractFrom(subRoot).singleOrNull()
                    ?.let { stringify(it) }

                matchType(value, expectedValue)
            }
            .forEach { subRoot ->
                subExtractor.extract(subRoot, output)
            }
    }

    override fun toStream(stream: JsonStream) {
        stream.beginObject()
        stream.name("path").value(path)
        stream.name("pathMode").value("FILTER")
        stream.name("matchType").value(matchType.name)
        if (expectedValue != null) {
            stream.name("value").value(expectedValue)
        }
        stream.name("subPath").value(subExtractor)
        stream.endObject()
    }

    override fun toString(): String {
        return "$path[$jsonFilterPath $matchType $expectedValue]$subExtractor"
    }

    internal enum class MatchType : (String?, String?) -> Boolean {
        EQUALS {
            override fun invoke(value: String?, expected: String?): Boolean {
                return value == expected
            }
        },
        NOT_EQUALS {
            override fun invoke(value: String?, expected: String?): Boolean {
                return value != expected
            }
        },
        IS_NULL {
            override fun invoke(value: String?, expected: String?): Boolean {
                return value == null
            }
        },
        NOT_NULL {
            override fun invoke(value: String?, expected: String?): Boolean {
                return value != null
            }
        }
    }
}

private const val HEX_RADIX = 16

internal class RelativeAddressExtractor(
    path: String,
) : JsonDataExtractor(path) {
    override fun extract(
        root: Map<String, *>,
        output: (String) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        val stackFrames = jsonPath.extractFrom(root)
            .filterIsInstance<Map<*, *>>()

        stackFrames.forEach { stackFrame ->
            val frameAddressString = stackFrame["frameAddress"] as? String
                ?: return
            val loadAddressString = stackFrame["loadAddress"] as? String
                ?: stackFrame["machoLoadAddress"] as? String
                ?: return

            val frameAddress = frameAddressString.parseAsAddress()
                ?: return
            val loadAddress = loadAddressString.parseAsAddress()
                ?: return

            val relativeAddress = frameAddress - loadAddress

            // the relativeAddress needs to be formatted as a 0xHex string for hashing
            val relativeAddressString = "0x${relativeAddress.toString(HEX_RADIX)}"
            output(relativeAddressString)
        }
    }

    private fun String.parseAsAddress(): ULong? {
        if (startsWith("0x"))
            return removePrefix("0x").toULongOrNull(HEX_RADIX)
        return toULongOrNull() ?: toULongOrNull(HEX_RADIX)
    }

    override fun toStream(stream: JsonStream) {
        stream.beginObject()
        stream.name("path").value(path)
        stream.name("pathMode").value("RELATIVE_ADDRESS")
        stream.endObject()
    }
}
