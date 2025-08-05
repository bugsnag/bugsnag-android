package com.bugsnag.android.internal

import android.util.JsonReader
import com.bugsnag.android.NoopLogger
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

internal class BugsnagMapperTest {
    private val bugsnagMapper = BugsnagMapper(NoopLogger)

    @Test
    fun mapsEventsBidirectionally() {
        repeat(8) { index ->
            val event = bugsnagMapper.convertToEvent(
                JsonReader(
                    this.javaClass
                        .getResourceAsStream("/event_serialization_$index.json")!!
                        .bufferedReader()
                ),
                "abc123"
            )

            val mappedEvent = JsonHelper.serialize(event)

            val secondaryEvent = bugsnagMapper.convertToEvent(
                JsonReader(ByteArrayInputStream(mappedEvent).reader()),
                "abc123"
            )
            val secondaryMappedEvent = JsonHelper.serialize(secondaryEvent)

            assertArrayEquals("event_serialization_$index.json", mappedEvent, secondaryMappedEvent)
        }
    }

//    @Test
//    fun mapsErrorsBidirectionally() {
//        repeat(3) { index ->
//            val contentMap = JsonHelper.deserialize(
//                this.javaClass.getResourceAsStream("/error_serialization_$index.json")!!
//            )
//
//            val error = bugsnagMapper.convertToError(contentMap)
//            val mappedError = bugsnagMapper.convertToMap(error)
//
//            assertEquals("error_serialization_$index.json", contentMap, mappedError)
//        }
//    }
}
