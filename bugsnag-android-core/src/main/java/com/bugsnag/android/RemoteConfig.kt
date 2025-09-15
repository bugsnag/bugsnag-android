package com.bugsnag.android

import android.util.JsonReader
import com.bugsnag.android.internal.DateUtils
import java.util.Date

internal class RemoteConfig(
    val configurationTag: String?,
    val configurationExpiry: Date?,
    val discardRules: List<DiscardRule>
) : JsonStream.Streamable {

    override fun toStream(stream: JsonStream) {
        stream.beginObject()
        stream.name(KEY_CONFIGURATION_TAG).value(configurationTag)
        configurationExpiry?.let { stream.name(KEY_CONFIG_EXPIRY).value(DateUtils.toIso8601(it)) }
        stream.name(KEY_DISCARD_RULES).beginArray()
        for (rule in discardRules) {
            stream.beginObject()
            when (rule) {
                is DiscardRule.All -> {
                    stream.name(MATCH_TYPE).value(DISCARD_RULE_ALL)
                }

                is DiscardRule.AllHandled -> {
                    stream.name(MATCH_TYPE).value(DISCARD_RULE_ALL_HANDLED)
                }
            }
            stream.endObject()
        }
        stream.endArray()
        stream.endObject()
    }

    internal companion object : JsonReadable<RemoteConfig> {
        internal const val KEY_CONFIGURATION_TAG = "configurationTag"
        internal const val KEY_CONFIG_EXPIRY = "configurationExpiry"
        internal const val KEY_DISCARD_RULES = "discardRules"
        internal const val DISCARD_RULE_ALL = "ALL"
        internal const val DISCARD_RULE_ALL_HANDLED = "ALL_HANDLED"
        internal const val MATCH_TYPE = "match_type"

        override fun fromReader(reader: JsonReader): RemoteConfig {
            var remoteConfig: RemoteConfig
            with(reader) {
                beginObject()
                var configurationTag: String = ""
                var configExpiry: Date = Date(0)
                var discardRules: List<DiscardRule> = emptyList()

                while (hasNext()) {
                    val key = nextName()
                    when (key) {
                        KEY_CONFIGURATION_TAG -> configurationTag = nextString()
                        KEY_CONFIG_EXPIRY -> configExpiry = DateUtils.fromIso8601(nextString())
                        KEY_DISCARD_RULES -> {
                            discardRules = parseInternals(reader)
                        }
                    }
                }
                remoteConfig = RemoteConfig(configurationTag, configExpiry, discardRules)
                endObject()
            }
            return remoteConfig
        }

        private fun parseInternals(reader: JsonReader): List<DiscardRule> {
            val rules = ArrayList<DiscardRule>()
            with(reader) {
                beginArray()
                while (hasNext()) {
                    val parsedRule = parseDiscardRule(reader)
                    parsedRule?.let { rules.add(it) }
                }
                endArray()
            }
            return rules
        }

        private fun parseDiscardRule(reader: JsonReader): DiscardRule? {
            var rule: DiscardRule? = null
            with(reader) {
                beginObject()
                beginArray()
                while (hasNext()) {
                    beginObject()
                    while (hasNext()) {
                        val key = nextName()
                        when (key) {
                            MATCH_TYPE -> {
                                when (nextString()) {
                                    DISCARD_RULE_ALL -> rule = DiscardRule.All()
                                    DISCARD_RULE_ALL_HANDLED -> rule = DiscardRule.AllHandled()
                                }
                            }
                        }
                        endObject()
                    }
                    endObject()
                }
                endArray()
                endObject()
            }
            return rule
        }
    }
}

internal abstract class DiscardRule {
    abstract fun shouldDiscard(payload: EventPayload): Boolean
    class All : DiscardRule() {
        override fun shouldDiscard(payload: EventPayload): Boolean = true
    }

    class AllHandled : DiscardRule() {
        override fun shouldDiscard(payload: EventPayload): Boolean = true
    }
}
