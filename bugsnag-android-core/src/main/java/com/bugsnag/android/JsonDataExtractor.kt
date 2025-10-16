package com.bugsnag.android

import com.bugsnag.android.internal.json.JsonCollectionPath
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Extracts data from JSON structures represented as collections objects (such as those returned
 * by [com.bugsnag.android.internal.JsonCollectionParser]). These are used when matching locally
 * stored events against remotely configured [DiscardRule]s.
 */
internal sealed class JsonDataExtractor(
    val path: String
) : JsonStream.Streamable {
    private lateinit var _jsonPath: JsonCollectionPath
    protected val jsonPath: JsonCollectionPath
        get() {
            if (!this::_jsonPath.isInitialized) {
                _jsonPath = JsonCollectionPath.fromString(path)
            }

            return _jsonPath
        }

    /**
     * Run this data extractor on the JSON object stored in [root] and send the result to [output].
     * [output] may be called any number of times depending on the content of [root] and the
     * rules defined in the specific extractor implementation.
     */
    abstract fun extract(
        root: Map<String, *>,
        output: (String) -> Unit
    )

    fun extract(root: Map<String, *>): List<String> {
        val out = ArrayList<String>()
        extract(root, out::add)
        return out
    }

    protected fun stringify(item: Any): String? =
        when (item) {
            is String -> item
            is Short, is Int, is Long, is Float, is Double, is Boolean -> item.toString()
            else -> null
        }

    companion object {
        fun fromJsonMap(json: Map<String, *>): JsonDataExtractor? {
            val pathMode = json["pathMode"] as? String

            return when (pathMode) {
                "REGEX" -> RegexExtractor.fromJsonMap(json)
                "FILTER" -> FilterExtractor.fromJsonMap(json)
                "RELATIVE_ADDRESS" -> RelativeAddressExtractor.fromJsonMap(json)
                "LITERAL", null -> LiteralPathExtractor.fromJsonMap(json)
                else -> null
            }
        }

        /**
         * Shorthand for `json.mapNotNull(this::fromJsonMap)`.
         */
        fun fromJsonList(json: List<Map<String, *>>): List<JsonDataExtractor> =
            json.mapNotNull(this::fromJsonMap)
    }
}

internal class LiteralPathExtractor(
    path: String
) : JsonDataExtractor(path) {
    override fun extract(
        root: Map<String, *>,
        output: (String) -> Unit,
    ) {
        jsonPath.extractFrom(root).forEach { item ->
            stringify(item)?.let { output(it) }
        }
    }

    override fun toStream(stream: JsonStream) {
        stream.beginObject()
        stream.name("path").value(path)
        stream.endObject()
    }

    override fun toString(): String = path

    companion object {
        fun fromJsonMap(json: Map<String, *>): LiteralPathExtractor? {
            val path = json["path"] as? String
                ?: return null
            return LiteralPathExtractor(path)
        }
    }
}

internal class RegexExtractor(
    path: String,
    regex: String
) : JsonDataExtractor(path) {
    private val regex: Pattern = regex.toPattern()

    override fun extract(
        root: Map<String, *>,
        output: (String) -> Unit
    ) {
        for (item in jsonPath.extractFrom(root)) {
            val stringValue =
                stringify(item)
                    ?: continue
            val matcher = regex.matcher(stringValue)

            // we *find* any match within the source string rather than require a full match
            if (matcher.find()) {
                output(extractGroups(stringValue, matcher))
            }
        }
    }

    private fun extractGroups(
        source: String,
        matcher: Matcher
    ): String {
        if (matcher.groupCount() > 0) {
            // the regex groups are joined together with a ',' separator
            val groups =
                buildString(source.length) {
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

    companion object {
        fun fromJsonMap(json: Map<String, *>): JsonDataExtractor? {
            val path = json["path"] as? String ?: return null
            val regex = json["regex"] as? String ?: return null

            return RegexExtractor(path, regex)
        }
    }
}

internal class FilterExtractor(
    path: String,
    val conditions: List<Condition>,
    val subExtractors: List<JsonDataExtractor>
) : JsonDataExtractor(path) {
    override fun extract(
        root: Map<String, *>,
        output: (String) -> Unit
    ) {
        jsonPath
            .extractFrom(root)
            .filterIsInstance<Map<String, *>>()
            .filter { subRoot -> conditions.all { it(subRoot) } }
            .forEach { subRoot ->
                subExtractors.forEach { extractor ->
                    extractor.extract(subRoot, output)
                }
            }
    }

    override fun toStream(stream: JsonStream) {
        stream.beginObject()
        stream.name("path").value(path)
        stream.name("pathMode").value("FILTER")

        stream.name("filter").beginObject()
        stream.name("conditions").value(conditions)
        stream.name("subPaths").value(subExtractors)
        stream.endObject()

        stream.endObject()
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromJsonMap(json: Map<String, *>): JsonDataExtractor? {
            val path = json["path"] as? String ?: return null
            val filter = json["filter"] as? Map<String, *> ?: return null

            val conditions = (filter["conditions"] as? List<*>)
                ?.filterIsInstance<Map<String, *>>()
                ?.mapNotNull { Condition.fromJsonMap(it) }
                // we require at least 1 valid condition
                .takeUnless { it.isNullOrEmpty() }
                ?: return null

            val subExtractors = (filter["subPaths"] as? List<*>)
                ?.filterIsInstance<Map<String, *>>()
                ?.mapNotNull { JsonDataExtractor.fromJsonMap(it) }
                // and we require at least one valid subPath
                .takeUnless { it.isNullOrEmpty() }
                ?: return null

            return FilterExtractor(
                path,
                conditions,
                subExtractors,
            )
        }
    }

    internal class Condition(
        val filterPath: JsonDataExtractor,
        val matchType: MatchType,
        val expectedValue: String
    ) : (Map<String, *>) -> Boolean, JsonStream.Streamable {
        override fun invoke(jsonObject: Map<String, *>): Boolean {
            val values = filterPath.extract(jsonObject)
            val extractedValue = values.firstOrNull()

            return matchType(extractedValue, expectedValue)
        }

        override fun toStream(stream: JsonStream) {
            stream.beginObject()
            stream.name("filterPath").value(filterPath)
            stream.name("matchType").value(matchType.name)
            stream.name("value").value(expectedValue)
            stream.endObject()
        }

        companion object {
            fun fromJsonMap(json: Map<String, *>): Condition? {
                @Suppress("UNCHECKED_CAST")
                val filterPath =
                    (json["filterPath"] as? Map<String, *>)
                        ?.let { JsonDataExtractor.fromJsonMap(it) }
                        ?: return null

                val matchType =
                    try {
                        (json["matchType"] as? String)?.let { MatchType.valueOf(it) }
                    } catch (_: Exception) {
                        null
                    } ?: return null

                val expectedValue =
                    json["value"] as? String
                        ?: return null

                return Condition(filterPath, matchType, expectedValue)
            }
        }

        internal enum class MatchType : (String?, String) -> Boolean {
            EQUALS {
                override fun invoke(
                    value: String?,
                    expected: String
                ): Boolean = value == expected
            },
            NOT_EQUALS {
                override fun invoke(
                    value: String?,
                    expected: String
                ): Boolean = value != expected
            },
            IS_NULL {
                override fun invoke(
                    value: String?,
                    expected: String
                ): Boolean =
                    if (expected == "true") {
                        value == null
                    } else {
                        value != null
                    }
            },
        }
    }
}

private const val HEX_RADIX = 16

internal class RelativeAddressExtractor(
    path: String
) : JsonDataExtractor(path) {
    override fun extract(
        root: Map<String, *>,
        output: (String) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        val stackFrames =
            jsonPath
                .extractFrom(root)
                .filterIsInstance<Map<*, *>>()

        stackFrames.forEach { stackFrame ->
            val frameAddressString =
                stackFrame["frameAddress"] as? String
                    ?: return
            val loadAddressString =
                stackFrame["loadAddress"] as? String
                    ?: stackFrame["machoLoadAddress"] as? String
                    ?: return

            val frameAddress =
                frameAddressString.parseAsAddress()
                    ?: return
            val loadAddress =
                loadAddressString.parseAsAddress()
                    ?: return

            val relativeAddress = frameAddress - loadAddress

            // the relativeAddress needs to be formatted as a 0xHex string for hashing
            val relativeAddressString = "0x${relativeAddress.toString(HEX_RADIX)}"
            output(relativeAddressString)
        }
    }

    private fun String.parseAsAddress(): ULong? {
        if (startsWith("0x")) {
            return removePrefix("0x").toULongOrNull(HEX_RADIX)
        }
        return toULongOrNull() ?: toULongOrNull(HEX_RADIX)
    }

    override fun toStream(stream: JsonStream) {
        stream.beginObject()
        stream.name("path").value(path)
        stream.name("pathMode").value("RELATIVE_ADDRESS")
        stream.endObject()
    }

    companion object {
        fun fromJsonMap(json: Map<String, *>): RelativeAddressExtractor? {
            val path = json["path"] as? String
                ?: return null
            return RelativeAddressExtractor(path)
        }
    }
}
