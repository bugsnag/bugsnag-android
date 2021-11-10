package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class EventFeatureFlagsCloneTest {

    @Test
    fun testFeatureFlagsClone() {
        val featureFlags = FeatureFlags()
        featureFlags.addFeatureFlag("sample_group", "123")

        val handledState = SeverityReason.newInstance(
            SeverityReason.REASON_HANDLED_EXCEPTION
        )
        val config = BugsnagTestUtils.generateImmutableConfig()
        val event = Event(
            RuntimeException(),
            config,
            handledState,
            Metadata(),
            featureFlags,
            NoopLogger
        )

        event.addFeatureFlag("demo_mode")

        // featureFlags objects should be deep copied
        assertNotSame(featureFlags, event.impl.featureFlags)

        // validate origin featureFlags
        val origExpected = listOf(FeatureFlag("sample_group", "123"))
        assertEquals(origExpected, featureFlags.toList())

        // validate event featureFlags
        val eventExpected = listOf(
            FeatureFlag("sample_group", "123"),
            FeatureFlag("demo_mode")
        )
        assertEquals(eventExpected, event.impl.featureFlags.toList())
    }
}
