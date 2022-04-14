package com.bugsnag.android.internal

import com.bugsnag.android.NoopLogger
import org.junit.Assert.assertEquals
import org.junit.Test

internal class BugsnagMapperTest {
    private val bugsnagMapper = BugsnagMapper(NoopLogger)

    @Test
    fun mapsEventsBidirectionally() {
        repeat(8) { index ->
            val contentMap = JsonHelper.deserialize(
                this.javaClass.getResourceAsStream("/event_serialization_$index.json")!!
            )

            val event = bugsnagMapper.convertToEvent(contentMap, "abc123")
            val mappedEvent = bugsnagMapper.convertToMap(event)

            assertEquals("event_serialization_$index.json", contentMap, mappedEvent)
        }
    }

    @Test
    fun mapsErrorsBidirectionally() {
        repeat(3) { index ->
            val contentMap = JsonHelper.deserialize(
                this.javaClass.getResourceAsStream("/error_serialization_$index.json")!!
            )

            val error = bugsnagMapper.convertToError(contentMap)
            val mappedError = bugsnagMapper.convertToMap(error)

            assertEquals("error_serialization_$index.json", contentMap, mappedError)
        }
    }
}
