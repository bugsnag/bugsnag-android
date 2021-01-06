package com.bugsnag.android.mazerunner

import android.content.Intent
import com.bugsnag.android.Configuration

data class BugsnagIntentParams(
    val eventType: String?,
    val apiKey: String,
    val notify: String,
    val sessions: String,
    val eventMetadata: String?
) {

    fun encode(intent: Intent) {
        with(intent) {
            putExtra(EXTRA_EVENT_TYPE, eventType)
            putExtra(EXTRA_API_KEY, apiKey)
            putExtra(EXTRA_NOTIFY, notify)
            putExtra(EXTRA_SESSIONS, sessions)
            putExtra(EXTRA_METADATA, eventMetadata)
        }
    }

    companion object {
        private const val EXTRA_EVENT_TYPE = "EXTRA_EVENT_TYPE"
        private const val EXTRA_API_KEY = "EXTRA_API_KEY"
        private const val EXTRA_NOTIFY = "EXTRA_NOTIFY"
        private const val EXTRA_SESSIONS = "EXTRA_SESSIONS"
        private const val EXTRA_METADATA = "EXTRA_METADATA"

        fun fromConfig(
            config: Configuration,
            eventType: String?,
            eventMetadata: String?
        ): BugsnagIntentParams {
            return BugsnagIntentParams(
                eventType,
                config.apiKey,
                config.endpoints.notify,
                config.endpoints.sessions,
                eventMetadata
            )
        }

        fun decode(intent: Intent): BugsnagIntentParams {
            return BugsnagIntentParams(
                intent.getStringExtra(EXTRA_EVENT_TYPE),
                requireNotNull(intent.getStringExtra(EXTRA_API_KEY)),
                requireNotNull(intent.getStringExtra(EXTRA_NOTIFY)),
                requireNotNull(intent.getStringExtra(EXTRA_SESSIONS)),
                intent.getStringExtra(EXTRA_METADATA)
            )
        }
    }
}
