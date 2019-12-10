package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

/**
 * Verifies that method calls are forwarded onto the appropriate method on Event fields,
 * and that adequate sanitisation takes place.
 */
@RunWith(MockitoJUnitRunner::class)
internal class EventApiTest {

    lateinit var event: Event

    @Before
    fun setUp() {
        event = Event(
            RuntimeException(),
            generateImmutableConfig(),
            HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION)
        )
        event.setUser("1", "fred@example.com", "Fred")
    }

    @Test
    fun getUser() {
        assertEquals(event._user, event.getUser())
    }

    @Test
    fun setUser() {
        event.setUser("99", "boo@example.com", "Boo")
        assertEquals(User("99", "boo@example.com", "Boo"), event._user)
    }

    @Test
    fun setUserId() {
        event.setUserId("99")
        assertEquals(User("99", "fred@example.com", "Fred"), event._user)
    }

    @Test
    fun setUserEmail() {
        event.setUserEmail("joe@test.com")
        assertEquals(User("1", "joe@test.com", "Fred"), event._user)
    }

    @Test
    fun setUserName() {
        event.setUserName("Jacinta Barltrop")
        assertEquals(User("1", "fred@example.com", "Jacinta Barltrop"), event._user)
    }

    @Test
    fun addMetadataTopLevel() {
        event.addMetadata("foo", mapOf(Pair("wham", "bar")))
        assertEquals(mapOf(Pair("wham", "bar")), event.metadata.getMetadata("foo"))
    }

    @Test
    fun addMetadata() {
        event.addMetadata("foo", "wham", "bar")
        assertEquals("bar", event.metadata.getMetadata("foo", "wham"))
    }

    @Test
    fun clearMetadataTopLevel() {
        event.addMetadata("foo", mapOf(Pair("wham", "bar")))
        event.clearMetadata("foo")
        assertNull(event.metadata.getMetadata("foo"))
    }

    @Test
    fun clearMetadata() {
        event.addMetadata("foo", "wham", "bar")
        event.clearMetadata("foo", "wham")
        assertNull(event.metadata.getMetadata("foo", "wham"))
    }
}
