package com.bugsnag.android

import android.app.ActivityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AnrDetailsCollectorTest {

    private companion object {
        private const val PID_EXAMPLE = 5902
    }

    private val collector = AnrDetailsCollector()
    private val stateInfo = ActivityManager.ProcessErrorStateInfo()

    @Mock
    lateinit var am: ActivityManager

    @Mock
    lateinit var client: Client

    @Before
    fun setUp() {
        stateInfo.pid = PID_EXAMPLE
        stateInfo.tag = "com.bugsnag.android.example/.ExampleActivity"
        stateInfo.shortMsg = "Input dispatching timed out"
        stateInfo.longMsg = "ANR in com.bugsnag.android.example"
    }

    @Test
    fun exceptionReturnsNull() {
        Mockito.`when`(am.processesInErrorState).thenThrow(RuntimeException())
        assertNull(collector.captureProcessErrorState(am, 0))
    }

    @Test
    fun emptyListReturnsNull() {
        Mockito.`when`(am.processesInErrorState).thenReturn(listOf())
        assertNull(collector.captureProcessErrorState(am, 0))
    }

    @Test
    fun differentPidReturnsNull() {
        Mockito.`when`(am.processesInErrorState).thenReturn(listOf(stateInfo))
        val captureProcessErrorState = collector.captureProcessErrorState(am, 0)
        assertNull(captureProcessErrorState)
    }

    @Test
    fun samePidReturnsObj() {
        val second = ActivityManager.ProcessErrorStateInfo()
        Mockito.`when`(am.processesInErrorState).thenReturn(listOf(stateInfo, second))
        val captureProcessErrorState = collector.captureProcessErrorState(am, PID_EXAMPLE)
        assertEquals(stateInfo, captureProcessErrorState)
    }

    @Test
    fun anrDetailsAltered() {
        Mockito.`when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig())
        Mockito.`when`(client.getMetadataState()).thenReturn(BugsnagTestUtils.generateMetadataState())
        Mockito.`when`(client.getFeatureFlagState()).thenReturn(BugsnagTestUtils.generateFeatureFlagState())
        val event = NativeInterface.createEvent(
            RuntimeException("whoops"),
            client,
            SeverityReason.newInstance(SeverityReason.REASON_ANR)
        )
        collector.addErrorStateInfo(event, stateInfo)
        assertEquals(stateInfo.shortMsg.replace("ANR", ""), event.errors[0].errorMessage)
    }
}
