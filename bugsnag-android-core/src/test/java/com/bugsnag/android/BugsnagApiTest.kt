package com.bugsnag.android

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

/**
 * Verifies that method calls are forwarded onto the appropriate method on Client.
 */
@RunWith(MockitoJUnitRunner::class)
class BugsnagApiTest {

    @Mock
    lateinit var client: Client

    @Mock
    lateinit var context: Context

    private lateinit var logger: InterceptingLogger

    @Before
    fun setUp() {
        Bugsnag.client = client
        logger = InterceptingLogger()
        Bugsnag.logger = logger
    }

    @Test
    fun init1() {
        Bugsnag.init(context, "api-key")
        assertNotNull(logger.msg)
    }

    @Test
    fun init2() {
        Bugsnag.init(context, Configuration("api-key"))
        assertNotNull(logger.msg)
    }

    @Test
    fun getContext() {
        `when`(client.context).thenReturn("foo")
        assertEquals("foo", Bugsnag.getContext())
    }

    @Test
    fun setContext() {
        Bugsnag.setContext("Bar")
        verify(client, times(1)).context = "Bar"
    }

    @Test
    fun getUser() {
        Bugsnag.getUser()
        verify(client, times(1)).getUser()
    }

    @Test
    fun setUser() {
        Bugsnag.setUser("123", "Jane@example.com", "Jig")
        verify(client, times(1)).setUser("123", "Jane@example.com", "Jig")
    }

    @Test
    fun setUserId() {
        Bugsnag.setUserId("123")
        verify(client, times(1)).setUserId("123")
    }

    @Test
    fun setUserEmail() {
        Bugsnag.setUserEmail("foo@example.com")
        verify(client, times(1)).setUserEmail("foo@example.com")
    }

    @Test
    fun setUserName() {
        Bugsnag.setUserName("Bob")
        verify(client, times(1)).setUserName("Bob")
    }

    @Test
    fun addOnError() {
        Bugsnag.addOnError { true }
        Bugsnag.addOnError(OnError { true })
        verify(client, times(2)).addOnError(ArgumentMatchers.any())
    }

    @Test
    fun removeOnError() {
        Bugsnag.removeOnError { true }
        Bugsnag.removeOnError(OnError { true })
        verify(client, times(2)).removeOnError(ArgumentMatchers.any())
    }

    @Test
    fun addOnBreadcrumb() {
        Bugsnag.addOnBreadcrumb { true }
        Bugsnag.addOnBreadcrumb(OnBreadcrumb { true })
        verify(client, times(2)).addOnBreadcrumb(ArgumentMatchers.any())
    }

    @Test
    fun removeOnBreadcrumb() {
        Bugsnag.removeOnBreadcrumb { true }
        Bugsnag.removeOnBreadcrumb(OnBreadcrumb { true })
        verify(client, times(2)).removeOnBreadcrumb(ArgumentMatchers.any())
    }

    @Test
    fun addOnSession() {
        Bugsnag.addOnSession { true }
        Bugsnag.addOnSession(OnSession { true })
        verify(client, times(2)).addOnSession(ArgumentMatchers.any())
    }

    @Test
    fun removeOnSession() {
        Bugsnag.removeOnSession { true }
        Bugsnag.removeOnSession(OnSession { true })
        verify(client, times(2)).removeOnSession(ArgumentMatchers.any())
    }

    @Test
    fun notify1() {
        val exc = RuntimeException()
        Bugsnag.notify(exc)
        verify(client, times(1)).notify(exc)
    }

    @Test
    fun notify2() {
        val exc = RuntimeException()
        val onError = OnError { true }
        Bugsnag.notify(exc, onError)
        verify(client, times(1)).notify(exc, onError)
    }

    @Test
    fun notify3() {
        Bugsnag.notify("LeakException", "whoops", arrayOf())
        verify(client, times(1)).notify("LeakException", "whoops", arrayOf())
    }

    @Test
    fun notify4() {
        val onError = OnError { true }
        Bugsnag.notify("LeakException", "whoops", arrayOf(), onError)
        verify(client, times(1)).notify("LeakException", "whoops", arrayOf(), onError)
    }

    @Test
    fun addMetadataTopLevel() {
        Bugsnag.addMetadata("foo", "bar")
        verify(client, times(1)).addMetadata("foo", "bar")
    }

    @Test
    fun addMetadata() {
        Bugsnag.addMetadata("foo", "bar", "wham")
        verify(client, times(1)).addMetadata("foo", "bar", "wham")
    }

    @Test
    fun clearMetadataTopLevel() {
        Bugsnag.clearMetadata("foo")
        verify(client, times(1)).clearMetadata("foo")
    }

    @Test
    fun clearMetadata() {
        Bugsnag.clearMetadata("foo", "bar")
        verify(client, times(1)).clearMetadata("foo", "bar")
    }

    @Test
    fun getMetadataTopLevel() {
        Bugsnag.getMetadata("foo")
        verify(client, times(1)).getMetadata("foo")
    }

    @Test
    fun getMetadata() {
        Bugsnag.getMetadata("foo", "bar")
        verify(client, times(1)).getMetadata("foo", "bar")
    }

    @Test
    fun leaveBreadcrumb() {
        Bugsnag.leaveBreadcrumb("whoops")
        verify(client, times(1)).leaveBreadcrumb("whoops")
    }

    @Test
    fun leaveBreadcrumb1() {
        Bugsnag.leaveBreadcrumb("whoops", BreadcrumbType.LOG, mapOf())
        verify(client, times(1)).leaveBreadcrumb("whoops", BreadcrumbType.LOG, mapOf())
    }

    @Test
    fun startSession() {
        Bugsnag.startSession()
        verify(client, times(1)).startSession()
    }

    @Test
    fun resumeSession() {
        Bugsnag.resumeSession()
        verify(client, times(1)).resumeSession()
    }

    @Test
    fun pauseSession() {
        Bugsnag.pauseSession()
        verify(client, times(1)).pauseSession()
    }

    @Test(expected = IllegalStateException::class)
    fun nullClient() {
        Bugsnag.client = null
        Bugsnag.getClient()
    }

    private class InterceptingLogger : Logger {
        var msg: String? = null
        override fun w(msg: String) {
            this.msg = msg
        }
    }
}
