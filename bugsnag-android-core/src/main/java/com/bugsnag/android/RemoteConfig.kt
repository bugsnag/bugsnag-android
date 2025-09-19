package com.bugsnag.android

import android.util.JsonReader
import com.bugsnag.android.RemoteConfig.Companion.DISCARD_RULE_ALL
import com.bugsnag.android.RemoteConfig.Companion.DISCARD_RULE_ALL_HANDLED
import com.bugsnag.android.RemoteConfig.Companion.KEY_MATCH_TYPE
import com.bugsnag.android.internal.DateUtils
import java.util.Date

internal class RemoteConfig(
    val configurationTag: String,
    val configurationExpiry: Date,
    val discardRules: List<DiscardRule>
) : JsonStream.Streamable {

    override fun toStream(stream: JsonStream) {
        stream.beginObject()
        stream.name(KEY_CONFIGURATION_TAG).value(configurationTag)
        stream.name(KEY_CONFIG_EXPIRY).value(DateUtils.toIso8601(configurationExpiry))
        stream.name(KEY_DISCARD_RULES).value(discardRules)
        stream.endObject()
    }

    internal companion object : JsonReadable<RemoteConfig?> {
        internal const val KEY_CONFIGURATION_TAG = "configurationTag"
        internal const val KEY_CONFIG_EXPIRY = "configurationExpiry"
        internal const val KEY_DISCARD_RULES = "discardRules"
        internal const val DISCARD_RULE_ALL = "ALL"
        internal const val DISCARD_RULE_ALL_HANDLED = "ALL_HANDLED"
        internal const val KEY_MATCH_TYPE = "match_type"

        fun fromMap(
            map: Map<String, Any?>,
            configurationKey: String? = map[KEY_CONFIGURATION_TAG] as? String,
            configurationExpiry: Date? = (map[KEY_CONFIG_EXPIRY] as? String)?.let(DateUtils::fromIso8601)
        ): RemoteConfig? {
            if (configurationKey == null || configurationExpiry == null) {
                return null
            }

            @Suppress("UNCHECKED_CAST")
            return RemoteConfig(
                configurationKey,
                configurationExpiry,
                (map[KEY_DISCARD_RULES] as List<Map<String, Any>>)
                    .mapNotNull { DiscardRule.fromMap(it) }
            )
        }

        override fun fromReader(reader: JsonReader): RemoteConfig? {
            var configurationTag: String? = null
            var configurationExpiry: Date? = null
            var discardRules: List<DiscardRule> = emptyList()
            reader.beginObject()
            while (reader.hasNext()) {
                val key = reader.nextName()
                when (key) {
                    KEY_CONFIGURATION_TAG -> configurationTag = reader.nextString()
                    KEY_CONFIG_EXPIRY ->
                        configurationExpiry = DateUtils.fromIso8601(reader.nextString())

                    KEY_DISCARD_RULES -> discardRules = parseDiscardRules(reader)
                }
            }
            reader.endObject()

            return if (configurationTag != null && configurationExpiry != null) {
                RemoteConfig(
                    configurationTag,
                    configurationExpiry,
                    discardRules
                )
            } else {
                null
            }
        }

        private fun parseDiscardRules(reader: JsonReader): List<DiscardRule> {
            val rules = ArrayList<DiscardRule>()
            reader.beginArray()
            while (reader.hasNext()) {
                val rule = DiscardRule.fromReader(reader)
                rule?.let { rules.add(it) }
            }
            reader.endArray()
            return rules
        }
    }
}

internal sealed class DiscardRule : JsonStream.Streamable {
    companion object : JsonReadable<DiscardRule?> {
        fun fromMap(map: Map<String, Any?>): DiscardRule? {
            return when (map[KEY_MATCH_TYPE]) {
                DISCARD_RULE_ALL -> All()
                DISCARD_RULE_ALL_HANDLED -> AllHandled()
                else -> null
            }
        }

        override fun fromReader(reader: JsonReader): DiscardRule? {
            var rule: DiscardRule? = null

            reader.beginObject()
            while (reader.hasNext()) {
                val key = reader.nextName()
                when (key) {
                    KEY_MATCH_TYPE -> {
                        when (reader.nextString()) {
                            DISCARD_RULE_ALL -> rule = All()
                            DISCARD_RULE_ALL_HANDLED -> rule = AllHandled()
                        }
                    }
                }
            }
            reader.endObject()
            return rule
        }
    }

    abstract fun shouldDiscard(payload: EventPayload): Boolean
    class All : DiscardRule() {
        override fun shouldDiscard(payload: EventPayload): Boolean = true

        override fun toStream(stream: JsonStream) {
            stream.beginObject()
            stream.name(KEY_MATCH_TYPE).value(DISCARD_RULE_ALL)
            stream.endObject()
        }
    }

    class AllHandled : DiscardRule() {
        override fun shouldDiscard(payload: EventPayload): Boolean {
            return payload.event?.isUnhandled != true
        }

        override fun toStream(stream: JsonStream) {
            stream.beginObject()
            stream.name(KEY_MATCH_TYPE).value(DISCARD_RULE_ALL_HANDLED)
            stream.endObject()
        }
    }
}
