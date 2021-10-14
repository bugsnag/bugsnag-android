package com.bugsnag.android

import com.bugsnag.android.internal.journal.JsonHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class ErrorDeserializationTest {
    companion object {
        @JvmStatic
        @Parameters
        fun testCases() = (0..1).map {
            "event_deserialization_$it.json" to "event_deserialization_expected_$it.json"
        }
    }

    @Parameter
    lateinit var resource: Pair<String, String>

    @Test
    fun testJsonSerialisation() {
        val (inputResource, expectedResource) = resource
        val jsonContent = JsonParser().read(inputResource)
        val map = JsonHelper.deserialize(jsonContent.toByteArray())
        val eventInternal = BugsnagJournalEventMapper(NoopLogger).convertToEventImpl(map, "api-key")
        val event = Event(eventInternal, NoopLogger)

        verifyJsonMatches(event, expectedResource)
    }
}
