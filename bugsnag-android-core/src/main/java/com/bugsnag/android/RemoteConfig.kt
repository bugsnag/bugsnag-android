package com.bugsnag.android

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

    internal companion object {
        internal const val KEY_CONFIGURATION_TAG = "configurationTag"
        internal const val KEY_CONFIG_EXPIRY = "configurationExpiry"
        internal const val KEY_DISCARD_RULES = "discardRules"
        internal const val KEY_MATCH_TYPE = "matchType"

        internal const val DISCARD_RULE_ALL = "ALL"
        internal const val DISCARD_RULE_ALL_HANDLED = "ALL_HANDLED"

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
    }
}

internal sealed class DiscardRule : JsonStream.Streamable {
    abstract fun shouldDiscard(payload: EventPayload): Boolean

    class All : DiscardRule() {
        override fun shouldDiscard(payload: EventPayload): Boolean = true

        override fun toStream(stream: JsonStream) {
            stream.beginObject()
            stream.name(KEY_MATCH_TYPE).value(DISCARD_RULE_ALL)
            stream.endObject()
        }

        override fun toString(): String = DISCARD_RULE_ALL
    }

    class AllHandled : DiscardRule() {
        override fun shouldDiscard(payload: EventPayload): Boolean = !payload.isUnhandled

        override fun toStream(stream: JsonStream) {
            stream.beginObject()
            stream.name(KEY_MATCH_TYPE).value(DISCARD_RULE_ALL_HANDLED)
            stream.endObject()
        }

        override fun toString(): String = DISCARD_RULE_ALL_HANDLED
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): DiscardRule? {
            return when (map[KEY_MATCH_TYPE]) {
                DISCARD_RULE_ALL -> All()
                DISCARD_RULE_ALL_HANDLED -> AllHandled()
                else -> null
            }
        }
    }
}
