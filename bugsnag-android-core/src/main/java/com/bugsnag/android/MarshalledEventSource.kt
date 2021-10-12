package com.bugsnag.android

import com.bugsnag.android.internal.journal.JsonHelper
import java.io.File

internal class MarshalledEventSource(
    private val apiKey: String,
    private val eventFile: File,
    private val logger: Logger
) : () -> Event {

    /**
     * The parsed and possibly processed event. This field remains `null` if the `EventSource`
     * is not used, and may not reflect the same data as is stored in `eventFile` (as the `Event`
     * is mutable, and may have been modified after loading).
     */
    var event: Event? = null
        private set

    override fun invoke(): Event {
        var unmarshalledEvent = event
        if (unmarshalledEvent == null) {
            unmarshalledEvent = unmarshall()
            event = unmarshalledEvent
        }

        return unmarshalledEvent
    }

    fun clear() {
        event = null
    }

    private fun unmarshall(): Event {
        val json = JsonHelper.deserialize(eventFile)
        val eventInternal = BugsnagJournalEventMapper(logger).convertToEventImpl(json, apiKey)
        requireNotNull(eventInternal)
        return Event(eventInternal, logger)
    }
}
