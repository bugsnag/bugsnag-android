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
    private lateinit var error: Error

    @Mock
    lateinit var am: ActivityManager

    @Before
    fun setUp() {
        stateInfo.pid = PID_EXAMPLE
        stateInfo.tag = "com.bugsnag.android.example/.ExampleActivity"
        stateInfo.shortMsg = "ANR Input dispatching timed out"
        stateInfo.longMsg = "ANR in com.bugsnag.android.example"

        error = Error.Builder(
            Configuration("f"),
            RuntimeException(),
            null,
            Thread.currentThread(),
            true
        ).build()
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
        collector.addErrorStateInfo(error, stateInfo)
        assertEquals(stateInfo.shortMsg.replace("ANR", ""), error.exceptionMessage)
    }
}
