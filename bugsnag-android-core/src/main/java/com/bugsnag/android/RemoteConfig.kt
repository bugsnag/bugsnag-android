package com.bugsnag.android

import com.bugsnag.android.RemoteConfig.Companion.DISCARD_RULE_ALL
import com.bugsnag.android.RemoteConfig.Companion.DISCARD_RULE_ALL_HANDLED
import com.bugsnag.android.RemoteConfig.Companion.DISCARD_RULE_HASH
import com.bugsnag.android.RemoteConfig.Companion.KEY_HASH
import com.bugsnag.android.RemoteConfig.Companion.KEY_MATCH_TYPE
import com.bugsnag.android.internal.DateUtils
import com.bugsnag.android.internal.JsonCollectionParser
import com.bugsnag.android.internal.toHexString

import java.security.MessageDigest
import java.util.Date

internal class RemoteConfig(
    val configurationTag: String,
    val configurationExpiry: Date,
    val discardRules: List<DiscardRule>
) : JsonStream.Streamable {

    override fun toStream(stream: JsonStream) {
        stream.beginObject()
        stream.name(KEY_CONFIG_TAG).value(configurationTag)
        stream.name(KEY_CONFIG_EXPIRY).value(DateUtils.toIso8601(configurationExpiry))
        stream.name(KEY_DISCARD_RULES).value(discardRules)
        stream.endObject()
    }

    internal companion object {
        internal const val KEY_CONFIG_TAG = "configurationTag"
        internal const val KEY_CONFIG_EXPIRY = "configurationExpiry"
        internal const val KEY_DISCARD_RULES = "discardRules"
        internal const val KEY_MATCH_TYPE = "matchType"
        internal const val KEY_HASH = "hash"

        internal const val DISCARD_RULE_ALL = "ALL"
        internal const val DISCARD_RULE_HASH = "HASH"
        internal const val DISCARD_RULE_ALL_HANDLED = "ALL_HANDLED"

        fun fromJsonMap(json: Map<String, *>): RemoteConfig? {
            val configurationTag = json[KEY_CONFIG_TAG] as? String
                ?: return null
            val configurationExpiry = json[KEY_CONFIG_EXPIRY] as? String
                ?: return null

            return try {
                fromJsonMap(
                    configurationTag,
                    DateUtils.fromIso8601(configurationExpiry),
                    json
                )
            } catch (_: Exception) {
                null
            }
        }

        fun fromJsonMap(
            configurationTag: String,
            configurationExpiry: Date,
            json: Map<String, *>
        ): RemoteConfig? {
            @Suppress("UNCHECKED_CAST")
            val discardRules = (json[KEY_DISCARD_RULES] as? List<Map<String, *>>).orEmpty()
                .mapNotNull { DiscardRule.fromJsonMap(it) }

            return RemoteConfig(configurationTag, configurationExpiry, discardRules)
        }
    }
}

internal sealed class DiscardRule : JsonStream.Streamable {

    abstract fun shouldDiscard(payload: EventPayload): Boolean

    companion object {
        fun fromJsonMap(json: Map<String, *>): DiscardRule? {
            val matchType = json[KEY_MATCH_TYPE] as? String

            return when (matchType) {
                DISCARD_RULE_ALL -> All
                DISCARD_RULE_ALL_HANDLED -> AllHandled
                DISCARD_RULE_HASH -> {
                    @Suppress("UNCHECKED_CAST")
                    val hashConfig = json[KEY_HASH] as? Map<String, *>
                    hashConfig?.let { Hash.fromJsonMap(it) }
                }

                else -> null
            }
        }
    }

    object All : DiscardRule() {
        override fun shouldDiscard(payload: EventPayload): Boolean = true
        override fun toStream(stream: JsonStream) {
            stream.beginObject()
            stream.name(KEY_MATCH_TYPE).value(DISCARD_RULE_ALL)
            stream.endObject()
        }
    }

    object AllHandled : DiscardRule() {
        override fun shouldDiscard(payload: EventPayload): Boolean {
            return !(payload.event?.isUnhandled ?: true)
        }

        override fun toStream(stream: JsonStream) {
            stream.beginObject()
            stream.name(KEY_MATCH_TYPE).value(DISCARD_RULE_ALL_HANDLED)
            stream.endObject()
        }
    }

    class Hash(
        private val unparsedPaths: List<Map<String, *>>,
        private val hashes: Set<String>
    ) : DiscardRule() {
        private val paths: List<JsonDataExtractor> by lazy {
            JsonDataExtractor.fromJsonList(unparsedPaths)
        }

        override fun shouldDiscard(payload: EventPayload): Boolean {
            try {
                val jsonParser = JsonCollectionParser(payload.toByteArray().inputStream())

                @Suppress("UNCHECKED_CAST")
                val json = jsonParser.parse() as? Map<String, Any>
                    ?: return false

                val output = HashPathOutput()
                paths.forEach { path -> path.extract(json, output) }

                return hashes.contains(output.toString())
            } catch (_: Exception) {
                return false
            }
        }

        override fun toStream(stream: JsonStream) {
            stream.beginObject()
            stream.name(KEY_MATCH_TYPE).value(DISCARD_RULE_HASH)

            stream.name("hash").beginObject()
            stream.name("paths").value(unparsedPaths)
            stream.name("matches").value(hashes)
            stream.endObject() // "hash"

            stream.endObject()
        }

        private class HashPathOutput : (String) -> Unit {
            private val separator = ';'.code.toByte()
            private val digest = MessageDigest.getInstance("SHA-1")

            private var firstItem = true

            override fun toString(): String {
                return digest.digest().toHexString()
            }

            override fun invoke(item: String) {
                if (!firstItem) {
                    digest.update(separator)
                }

                digest.update(item.toByteArray())
                firstItem = false
            }
        }

        companion object {
            fun fromJsonMap(json: Map<String, *>): Hash? {
                @Suppress("UNCHECKED_CAST")
                val paths = json["paths"] as? List<Map<String, *>>
                    ?: return null

                @Suppress("UNCHECKED_CAST")
                val matches = json["matches"] as? List<String>
                    ?: return null

                return Hash(
                    paths,
                    matches.filterIsInstanceTo(HashSet())
                )
            }
        }
    }
}
