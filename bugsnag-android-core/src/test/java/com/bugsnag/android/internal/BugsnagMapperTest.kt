package com.bugsnag.android.internal

import com.bugsnag.android.NoopLogger
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class BugsnagMapperTest {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun testCases(): Collection<String> = listOf(
            "event_serialization_0.json",
            "event_serialization_1.json",
            "event_serialization_2.json",
            "event_serialization_3.json",
            "event_serialization_4.json",
            "event_serialization_5.json",
            "event_serialization_6.json",
            "event_serialization_7.json"
        )
    }

    @Parameterized.Parameter
    lateinit var serializedEventName: String

    private val bugsnagMapper = BugsnagMapper(NoopLogger)

    @Test
    fun mapsBidirectionally() {
        val contentMap = JsonHelper.deserialize(this.javaClass.getResourceAsStream("/$serializedEventName")!!)
        val event = bugsnagMapper.convertToEvent(contentMap, "abc123")
        val mappedEvent = bugsnagMapper.convertToMap(event)

        assertEquals(contentMap, mappedEvent)
    }
}
