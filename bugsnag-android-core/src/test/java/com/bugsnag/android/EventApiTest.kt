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
            SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION),
            NoopLogger
        )
        event.setUser("1", "fred@example.com", "Fred")
    }

    @Test
    fun getUser() {
        assertEquals(event.impl.userImpl, event.getUser())
    }

    @Test
    fun setUser() {
        event.setUser("99", "boo@example.com", "Boo")
        assertEquals(User("99", "boo@example.com", "Boo"), event.impl.userImpl)
    }

    @Test
    fun addMetadataTopLevel() {
        event.addMetadata("foo", mapOf(Pair("wham", "bar")))
        assertEquals(mapOf(Pair("wham", "bar")), event.impl.metadata.getMetadata("foo"))
    }

    @Test
    fun addMetadata() {
        event.addMetadata("foo", "wham", "bar")
        assertEquals("bar", event.impl.metadata.getMetadata("foo", "wham"))
    }

    @Test
    fun clearMetadataTopLevel() {
        event.addMetadata("foo", mapOf(Pair("wham", "bar")))
        event.clearMetadata("foo")
        assertNull(event.impl.metadata.getMetadata("foo"))
    }

    @Test
    fun clearMetadata() {
        event.addMetadata("foo", "wham", "bar")
        event.clearMetadata("foo", "wham")
        assertNull(event.impl.metadata.getMetadata("foo", "wham"))
    }

    @Test
    fun addFeatureFlagWithoutVariant() {
        event.addFeatureFlag("demo_mode")
        assertEquals(
            listOf(FeatureFlag("demo_mode")),
            event.impl.featureFlags.toList()
        )
    }

    @Test
    fun addFeatureFlag() {
        event.addFeatureFlag("sample_group", "a")
        assertEquals(
            listOf(FeatureFlag("sample_group", "a")),
            event.impl.featureFlags.toList()
        )
    }

    @Test
    fun clearFeatureFlag() {
        event.addFeatureFlag("demo_group")
        event.addFeatureFlag("sample_group", "a")
        event.clearFeatureFlag("demo_group")
        assertEquals(
            listOf(FeatureFlag("sample_group", "a")),
            event.impl.featureFlags.toList()
        )
    }

    @Test
    fun clearFeatureFlags() {
        event.addFeatureFlag("demo_group")
        event.addFeatureFlag("sample_group", "a")
        event.clearFeatureFlags()
        assertEquals(emptyList<FeatureFlag>(), event.impl.featureFlags.toList())
    }
}
