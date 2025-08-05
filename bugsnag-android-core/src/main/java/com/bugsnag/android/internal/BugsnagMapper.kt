package com.bugsnag.android.internal

import android.util.JsonReader
import com.bugsnag.android.BugsnagEventMapper
import com.bugsnag.android.Event
import com.bugsnag.android.Logger
import com.bugsnag.android.Error as BugsnagError

class BugsnagMapper(logger: Logger) {
    private val eventMapper = BugsnagEventMapper(logger)

    /**
     * Convert the given `JsonReader` to an `Event` object
     */
    fun convertToEvent(reader: JsonReader, fallbackApiKey: String): Event {
        return eventMapper.convertToEvent(reader, fallbackApiKey)
    }

    /**
     * Convert the given `JsonReader` to an `Error` object
     */
    fun convertToError(reader: JsonReader): BugsnagError {
        return eventMapper.convertError(reader)
    }
}
