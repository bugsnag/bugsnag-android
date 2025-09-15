package com.bugsnag.android

import android.util.JsonReader
import com.bugsnag.android.internal.DateUtils
import java.util.Date

internal class RemoteConfig(
    val configurationTag: String,
    val configExpiry: Date,
    val discardRules: List<DiscardRule>
) : JsonStream.Streamable {

    override fun toStream(stream: JsonStream) {
        stream.beginObject()
        stream.name("configurationTag").value(configurationTag)
        stream.name("configExpiry").value(DateUtils.toIso8601(configExpiry))
        stream.name("internals").beginObject()
        stream.name("discardRules").beginArray()
        for (rule in discardRules) {
            stream.beginObject()
            when (rule) {
                is DiscardRule.All -> {
                    stream.name("type").value("all")
                }

                is DiscardRule.AllHandled -> {
                    stream.name("type").value("allHandled")
                }
            }
            stream.endObject()
        }
        stream.endArray()
        stream.endObject()
    }

    internal companion object : JsonReadable<RemoteConfig> {
        private const val KEY_CONFIGURATION_TAG = "configurationTag"
        private const val KEY_CONFIG_EXPIRY = "configExpiry"
        private const val KEY_INTERNALS = "internals"
        private const val KEY_DISCARD_RULES = "discardRules"

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
                        KEY_INTERNALS -> {
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
                    beginObject()
                    val parsedRule = parseDiscardRule(reader)
                    parsedRule?.let { rules.add(it) }
                    endObject()
                }
                endArray()
            }
            return rules
        }

        private fun parseDiscardRule(reader: JsonReader): DiscardRule? {
            var rule: DiscardRule? = null
            with(reader) {
                beginArray()
                while (hasNext()) {
                    beginObject()
                    while (hasNext()) {
                        val key = nextName()
                        when (key) {
                            "match_type" -> {
                                when (nextString()) {
                                    "all" -> rule = DiscardRule.All()
                                    "allHandled" -> rule = DiscardRule.AllHandled()
                                }
                            }
                        }
                    }
                    endObject()
                }
                endArray()
            }
            return rule
        }
    }
}

sealed class DiscardRule {
    abstract fun shouldDiscard(payload: EventPayload): Boolean
    class All : DiscardRule() {
        override fun shouldDiscard(payload: EventPayload): Boolean = true
    }

    class AllHandled : DiscardRule() {
        override fun shouldDiscard(payload: EventPayload): Boolean = true
    }
}
