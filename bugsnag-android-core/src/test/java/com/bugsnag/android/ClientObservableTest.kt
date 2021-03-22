package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientObservableTest {

    private val clientObservable = ClientObservable()

    @Test
    fun postOrientationChange() {
        clientObservable.addObserver { _, arg ->
            val msg = arg as StateEvent.UpdateOrientation
            assertEquals("landscape", msg.orientation)
        }
        clientObservable.postOrientationChange("landscape")
    }

    @Test
    fun postNdkInstall() {
        clientObservable.postNdkInstall(BugsnagTestUtils.generateImmutableConfig(), "/foo", 0)
        clientObservable.addObserver { _, arg ->
            assertTrue(arg is StateEvent.Install)
        }
    }

    @Test
    fun postNdkDeliverPending() {
        clientObservable.postNdkDeliverPending()
        clientObservable.addObserver { _, arg ->
            assertTrue(arg is StateEvent.DeliverPending)
        }
    }
}
