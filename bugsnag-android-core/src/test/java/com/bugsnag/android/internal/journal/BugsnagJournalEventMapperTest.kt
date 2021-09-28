package com.bugsnag.android.internal.journal

import com.bugsnag.android.NoopLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BugsnagJournalEventMapperTest {

    @Test
    fun emptyMapNullEvent() {
        val mapper = BugsnagJournalEventMapper(NoopLogger)
        assertNull(mapper.convertToEvent(emptyMap()))
    }

    @Test
    fun populatedEvent() {
        val mapper = BugsnagJournalEventMapper(NoopLogger)
        val map = mapOf(
            "apiKey" to "my-api-key",
            "user" to mapOf(
                "id" to "123",
                "name" to "Boog Snoog",
                "email" to "hello@example.com"
            )
        )
        val event = mapper.convertToEvent(map)
        checkNotNull(event)
        val user = event.getUser()
        assertNotNull(user)
        assertEquals("123", user.id)
        assertEquals("Boog Snoog", user.name)
        assertEquals("hello@example.com", user.email)
    }
}
