package com.bugsnag.android

import com.bugsnag.android.internal.StateObserver
import com.bugsnag.android.internal.convertToImmutableConfig
import com.bugsnag.android.internal.dag.RunnableProvider
import com.bugsnag.android.internal.dag.ValueProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        clientObservable.addObserver(
            StateObserver {
                assertTrue(it is StateEvent.Install)
            }
        )
        clientObservable.postNdkInstall(BugsnagTestUtils.generateImmutableConfig(), "/foo", 0)
    }

    @Test
    fun postNdkInstallBuildUuidNull() {
        // When buildUuid is null, Install event should have null buildUuid
        val config = BugsnagTestUtils.generateImmutableConfig()
        var installEvent: StateEvent.Install? = null
        clientObservable.addObserver {
            if (it is StateEvent.Install) installEvent = it
        }
        clientObservable.postNdkInstall(config, "/foo", 0)
        assertNull(installEvent?.buildUuid)
    }

    @Test
    fun postNdkInstallBuildUuidComplete() {
        // When buildUuid is a ValueProvider (already complete), Install should include it
        val config = convertToImmutableConfig(
            BugsnagTestUtils.generateConfiguration(),
            ValueProvider("test-uuid")
        )
        var installEvent: StateEvent.Install? = null
        clientObservable.addObserver(
            StateObserver {
                if (it is StateEvent.Install) installEvent = it
            }
        )
        clientObservable.postNdkInstall(config, "/foo", 0)
        assertEquals("test-uuid", installEvent?.buildUuid)
    }

    @Test
    fun postNdkInstallBuildUuidPending() {
        // When buildUuid is a pending RunnableProvider, Install should have null buildUuid
        val pendingProvider = object : RunnableProvider<String?>() {
            override fun invoke(): String? = "deferred-uuid"
        }
        val config = convertToImmutableConfig(
            BugsnagTestUtils.generateConfiguration(),
            pendingProvider
        )
        var installEvent: StateEvent.Install? = null
        clientObservable.addObserver(
            StateObserver {
                if (it is StateEvent.Install) installEvent = it
            }
        )
        clientObservable.postNdkInstall(config, "/foo", 0)
        assertNull(installEvent?.buildUuid)
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
