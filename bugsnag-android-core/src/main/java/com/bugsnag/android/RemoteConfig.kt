package com.bugsnag.android

import com.bugsnag.android.RemoteConfig.Companion.DISCARD_RULE_ALL
import com.bugsnag.android.RemoteConfig.Companion.DISCARD_RULE_ALL_HANDLED
import com.bugsnag.android.RemoteConfig.Companion.DISCARD_RULE_HASH
import com.bugsnag.android.RemoteConfig.Companion.KEY_HASH
import com.bugsnag.android.RemoteConfig.Companion.KEY_MATCH_TYPE
import com.bugsnag.android.internal.DateUtils
import com.bugsnag.android.internal.JsonCollectionParser
import com.bugsnag.android.internal.remoteconfig.DeliveryConfiguration
import com.bugsnag.android.internal.remoteconfig.DeliveryConfiguration.Companion.ENCODING_GZIP
import com.bugsnag.android.internal.remoteconfig.DeliveryConfiguration.Companion.ENCODING_NONE
import com.bugsnag.android.internal.remoteconfig.DeliveryConfiguration.Companion.KEY_PAYLOAD_ENCODING
import com.bugsnag.android.internal.toHexString
import java.security.MessageDigest
import java.util.Date

internal class RemoteConfig(
    val configurationTag: String?,
    val configurationExpiry: Date,
    val deliveryConfig: DeliveryConfiguration?,
    val discardRules: List<DiscardRule>
) : JsonStream.Streamable {

    override fun toStream(stream: JsonStream) {
        stream.beginObject()
        if (configurationTag != null) {
            stream.name(KEY_CONFIG_TAG).value(configurationTag)
        }

        val delivery = deliveryConfig
        if (delivery != null) {
            stream.name(KEY_DELIVERY).beginObject()

            stream.name(KEY_PAYLOAD_ENCODING).value(
                when (delivery.payloadEncoding) {
                    DeliveryParams.PayloadEncoding.GZIP -> ENCODING_GZIP
                    else -> ENCODING_NONE
                }
            )

            stream.endObject()
        }

        stream.name(KEY_CONFIG_EXPIRY).value(DateUtils.toIso8601(configurationExpiry))
        stream.name(KEY_DISCARD_RULES).value(discardRules)
        stream.endObject()
    }

    internal companion object {
        internal const val KEY_CONFIG_TAG = "configurationTag"
        internal const val KEY_CONFIG_EXPIRY = "configurationExpiry"
        internal const val KEY_DISCARD_RULES = "discardRules"
        internal const val KEY_DELIVERY = "delivery"
        internal const val KEY_MATCH_TYPE = "matchType"
        internal const val KEY_HASH = "hash"

        internal const val DISCARD_RULE_ALL = "ALL"
        internal const val DISCARD_RULE_HASH = "HASH"
        internal const val DISCARD_RULE_ALL_HANDLED = "ALL_HANDLED"

        fun fromJsonMap(json: Map<String, *>): RemoteConfig? {
            val configurationTag = json[KEY_CONFIG_TAG] as? String
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
            configurationTag: String?,
            configurationExpiry: Date,
            json: Map<String, *>
        ): RemoteConfig {
            @Suppress("UNCHECKED_CAST")
            val discardRules = (json[KEY_DISCARD_RULES] as? List<Map<String, *>>)
                .orEmpty()
                .mapNotNull { DiscardRule.fromJsonMap(it) }

            @Suppress("UNCHECKED_CAST")
            val deliveryConfig = (json[KEY_DELIVERY] as? Map<String, *>)
                ?.let { DeliveryConfiguration.fromJsonMap(it) }

            return RemoteConfig(configurationTag, configurationExpiry, deliveryConfig, discardRules)
        }

        fun createEmpty(
            configurationTag: String?,
            configurationExpiry: Date,
        ): RemoteConfig {
            return RemoteConfig(configurationTag, configurationExpiry, null, emptyList())
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

        override fun toString(): String = DISCARD_RULE_ALL
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

        override fun toString(): String = DISCARD_RULE_ALL_HANDLED
    }

    data class Hash(
        private val unparsedPaths: List<*>,
        private val matches: Set<String>
    ) : DiscardRule() {
        internal val paths: List<JsonDataExtractor>? by lazy {
            JsonDataExtractor.fromJsonList(unparsedPaths)
                // avoid possible false-positives when some of the paths cannot be parsed
                .takeIf { it.size == unparsedPaths.size }
        }

        override fun shouldDiscard(payload: EventPayload): Boolean {
            try {
                val jsonParser = JsonCollectionParser(payload.toByteArray().inputStream())

                @Suppress("UNCHECKED_CAST")
                val json = jsonParser.parse() as? Map<String, Any>
                    ?: return false

                val eventsJson = json[KEY_EVENTS] as? List<*>
                    ?: return false

                @Suppress("UNCHECKED_CAST")
                val eventJson = eventsJson.singleOrNull() as? Map<String, Any>
                    ?: return false

                return shouldDiscardJson(eventJson)
            } catch (_: Exception) {
                return false
            }
        }

        internal fun shouldDiscardJson(json: Map<String, Any>): Boolean {
            val lPaths = paths ?: return false
            val hashString = calculatePayloadHash(lPaths, json)
            return matches.contains(hashString)
        }

        override fun toStream(stream: JsonStream) {
            stream.beginObject()
            stream.name(KEY_MATCH_TYPE).value(DISCARD_RULE_HASH)

            stream.name(KEY_HASH).beginObject()
            stream.name(KEY_PATHS).value(unparsedPaths)
            stream.name(KEY_MATCHES).value(matches)
            stream.endObject() // "hash"

            stream.endObject()
        }

        override fun toString(): String {
            return "Hash$matches"
        }

        companion object {
            const val KEY_PATHS = "paths"
            const val KEY_MATCHES = "matches"
            const val KEY_EVENTS = "events"

            internal fun calculatePayloadHash(
                paths: List<JsonDataExtractor>,
                json: Map<String, Any>
            ): String {
                val output = PathOutputHasher()
                paths.forEach { path -> path.extract(json, output) }
                val hashString = output.hashString()
                return hashString
            }

            fun fromJsonMap(json: Map<String, *>): Hash? {
                @Suppress("UNCHECKED_CAST")
                val paths = json[KEY_PATHS] as? List<Map<String, *>>
                    ?: return null

                @Suppress("UNCHECKED_CAST")
                val matches = json[KEY_MATCHES] as? List<String>
                    ?: return null

                return Hash(
                    paths,
                    matches.filterIsInstanceTo(HashSet())
                )
            }
        }
    }
}

internal class PathOutputHasher : (String) -> Unit {
    private val separator = ';'.code.toByte()
    private val digest = MessageDigest.getInstance("SHA-1")

    private var firstItem = true

    private var completeHashString: String? = null

    fun hashString(): String {
        var hash = completeHashString
        if (hash == null) {
            hash = digest.digest().toHexString()
            completeHashString = hash
        }

        return hash
    }

    override fun invoke(item: String) {
        if (!firstItem) {
            digest.update(separator)
        }

        digest.update(item.toByteArray())
        firstItem = false
    }

    override fun toString(): String {
        return hashString()
    }
}
