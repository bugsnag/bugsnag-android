package com.bugsnag.android.internal.journal

import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.BugsnagJournalEventMapper
import com.bugsnag.android.EventInternal
import com.bugsnag.android.NoopLogger
import com.bugsnag.android.internal.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BugsnagJournalEventMapperTest {

    private lateinit var journalMap: Map<String, Any?>
    private lateinit var minimalJournalMap: Map<String, Any?>
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
        val breadcrumbs = listOf(
            mapOf(
                "metaData" to emptyMap<String, Any?>(),
                "name" to "Bugsnag loaded",
                "type" to "state",
                "timestamp" to "2021-09-28T10:31:09.092Z"
            ),
            mapOf(
                "metaData" to mapOf(
                    "hasConnection" to true,
                    "networkState" to "wifi"
                ),
                "name" to "Connectivity changed",
                "type" to "request",
                "timestamp" to "2021-09-28T10:31:10.856Z"
            )
        )
        minimalJournalMap = mapOf(
            "apiKey" to "my-api-key",
            "user" to mapOf(
                "id" to "123",
                "name" to "Boog Snoog",
                "email" to "hello@example.com"
            ),
            "metaData" to mapOf(
                "app" to appSection,
                "foo" to fooSection
            ),
            "breadcrumbs" to breadcrumbs
        )
        journalMap = minimalJournalMap + ("context" to "ExampleActivity")
    }

    @Test
    fun emptyMapNullEvent() {
        val mapper = BugsnagJournalEventMapper(NoopLogger)
        assertNull(mapper.convertToEvent(emptyMap()))
    }

    /**
     * Validates that an event that omits non-mandatory fields such as
     * context/session is deserialized.
     */
    @Test
    fun populatedMinimalEvent() {
        val mapper = BugsnagJournalEventMapper(NoopLogger)
        val event = mapper.convertToEvent(minimalJournalMap)
        checkNotNull(event)
        validateMandatoryEventFields(event)

        // context/session
        assertNull(event.context)
        assertNull(event.session)
    }

    /**
     * Validates that an event containing non-mandatory fields such as
     * context/session is deserialized.
     */
    @Test
    fun populatedEvent() {
        val mapper = BugsnagJournalEventMapper(NoopLogger)
        val event = mapper.convertToEvent(journalMap)
        checkNotNull(event)
        validateMandatoryEventFields(event)

        // context
        assertEquals("ExampleActivity", event.context)
    }

    private fun validateMandatoryEventFields(event: EventInternal) {
        // user
        val user = event.getUser()
        assertNotNull(user)
        assertEquals("123", user.id)
        assertEquals("Boog Snoog", user.name)
        assertEquals("hello@example.com", user.email)

        // metadata
        assertEquals(appSection, event.getMetadata("app"))
        assertEquals(fooSection, event.getMetadata("foo"))

        // breadcrumbs
        assertEquals(2, event.breadcrumbs.size)

        with(event.breadcrumbs[0]) {
            assertEquals("Bugsnag loaded", message)
            assertEquals(BreadcrumbType.STATE, type)
            assertEquals(DateUtils.fromIso8601("2021-09-28T10:31:09.092Z"), timestamp)
            assertEquals(emptyMap<String, Any?>(), metadata)
        }

        with(event.breadcrumbs[1]) {
            assertEquals("Connectivity changed", message)
            assertEquals(BreadcrumbType.REQUEST, type)
            assertEquals(DateUtils.fromIso8601("2021-09-28T10:31:10.856Z"), timestamp)
            val expectedMetadata = mapOf(
                "hasConnection" to true,
                "networkState" to "wifi"
            )
            assertEquals(expectedMetadata, metadata)
        }
    }
}
