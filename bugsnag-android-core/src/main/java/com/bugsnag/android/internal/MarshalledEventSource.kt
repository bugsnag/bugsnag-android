package com.bugsnag.android.internal

import com.bugsnag.android.Event
import java.io.File

internal class MarshalledEventSource(private val eventFile: File) : () -> Event {

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

    @Suppress("StopShip")
    private fun unmarshall(): Event {
        TODO("Not yet implemented")
    }
}
