package com.bugsnag.android.internal.journal

import com.bugsnag.android.NoopLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BugsnagJournalEventMapperTest {

    private lateinit var journalMap: Map<String, Any?>
    private lateinit var fooSection: Map<String, Any?>
    private lateinit var appSection: Map<String, Any?>

    @Before
    fun setUp() {
        fooSection = mapOf(
            "map" to mapOf(
                "a" to "z"
            ),
            "array" to listOf(
                "1", "2", "3"
            )
        )
        appSection = mapOf(
            "processName" to "com.example.bugsnag.android",
            "memoryLimit" to 40265318449,
            "lowMemory" to false
        )
        journalMap = mapOf(
            "apiKey" to "my-api-key",
            "user" to mapOf(
                "id" to "123",
                "name" to "Boog Snoog",
                "email" to "hello@example.com"
            ),
            "metaData" to mapOf(
                "app" to appSection,
                "foo" to fooSection
            )
        )
    }

    @Test
    fun emptyMapNullEvent() {
        val mapper = BugsnagJournalEventMapper(NoopLogger)
        assertNull(mapper.convertToEvent(emptyMap()))
    }

    @Test
    fun populatedEvent() {
        val mapper = BugsnagJournalEventMapper(NoopLogger)
        val event = mapper.convertToEvent(journalMap)
        checkNotNull(event)

        // user
        val user = event.getUser()
        assertNotNull(user)
        assertEquals("123", user.id)
        assertEquals("Boog Snoog", user.name)
        assertEquals("hello@example.com", user.email)

        // metadata
        assertEquals(appSection, event.getMetadata("app"))
        assertEquals(fooSection, event.getMetadata("foo"))
    }
}
