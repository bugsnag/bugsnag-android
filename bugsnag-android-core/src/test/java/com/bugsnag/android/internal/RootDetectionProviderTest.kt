package com.bugsnag.android.internal

import com.bugsnag.android.ClientObservable
import com.bugsnag.android.DeviceBuildInfo
import com.bugsnag.android.NoopLogger
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class RootDetectionProviderTest {

    @Test
    fun rootDetectionIsDeferredUntilItsResultIsRequested() {
        val provider = RootDetectionProvider(
            DeviceBuildInfo(null, null, null, null, null, null, "test-keys", null, null),
            ClientObservable(),
            NoopLogger
        )

        assertFalse(provider.isComplete)
        assertTrue(provider.getRootDetectionResult()!!)
        assertTrue(provider.isComplete)

        // A cached result does not re-run detection.
        assertTrue(provider.getRootDetectionResult()!!)
    }

    @Test
    fun backgroundRootDetectionCompletesWithinFiveSeconds() {
        val provider = RootDetectionProvider(
            DeviceBuildInfo(null, null, null, null, null, null, "test-keys", null, null),
            ClientObservable(),
            NoopLogger
        )

        provider.startInBackground()
        val deadline = System.currentTimeMillis() + 5_000
        while (!provider.isComplete && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }

        assertTrue(provider.isComplete)
        assertTrue(provider.getRootDetectionResult()!!)
    }
}
