package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test

class ClientObservableTest {

    private val clientObservable = ClientObservable()

    @Test
    fun postOrientationChange() {
        clientObservable.addObserver { _, arg ->
            val msg = arg as NativeInterface.Message
            assertEquals(NativeInterface.MessageType.UPDATE_ORIENTATION, msg.type)
            assertEquals(90, msg.value)
        }
        clientObservable.postOrientationChange(90)
    }

    @Test
    fun postNdkInstall() {
        clientObservable.postNdkInstall(BugsnagTestUtils.generateImmutableConfig())
        clientObservable.addObserver { _, arg ->
            val msg = arg as NativeInterface.Message
            assertEquals(NativeInterface.MessageType.INSTALL, msg.type)
        }
    }

    @Test
    fun postNdkDeliverPending() {
        clientObservable.postNdkDeliverPending()
        clientObservable.addObserver { _, arg ->
            val msg = arg as NativeInterface.Message
            assertEquals(NativeInterface.MessageType.DELIVER_PENDING, msg.type)
        }
    }
}
