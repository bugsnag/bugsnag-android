package com.bugsnag.android

import com.bugsnag.android.internal.StateObserver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientObservableTest {

    private val clientObservable = ClientObservable()

    @Test
    fun postOrientationChange() {
        clientObservable.addObserver(
            StateObserver {
                val msg = it as StateEvent.UpdateOrientation
                assertEquals("landscape", msg.orientation)
            }
        )
        clientObservable.postOrientationChange("landscape")
    }

    @Test
    fun postNdkInstall() {
        clientObservable.postNdkInstall(BugsnagTestUtils.generateImmutableConfig(), "/foo", 0)
        clientObservable.addObserver(
            StateObserver {
                assertTrue(it is StateEvent.Install)
            }
        )
    }

    @Test
    fun postNdkDeliverPending() {
        clientObservable.postNdkDeliverPending()
        clientObservable.addObserver(
            StateObserver {
                assertTrue(it is StateEvent.DeliverPending)
            }
        )
    }
}
