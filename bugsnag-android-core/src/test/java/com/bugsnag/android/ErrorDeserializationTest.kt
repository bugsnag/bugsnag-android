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
        fun testCases() = (0..0).map { "event_deserialization_$it.json" }
    }

    @Parameter
    lateinit var resourceName: String

    @Test
    fun testJsonSerialisation() {
        val jsonContent = JsonParser().read(resourceName)
        val map = JsonHelper.deserialize(jsonContent.toByteArray())
        val eventInternal = BugsnagJournalEventMapper(NoopLogger).convertToEventImpl(map, "api-key")
        val event = Event(eventInternal, NoopLogger)

        verifyJsonMatches(event, resourceName)
    }
}
