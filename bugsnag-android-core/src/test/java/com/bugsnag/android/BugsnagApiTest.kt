package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

/**
 * Verifies that method calls are forwarded onto the appropriate method on Client.
 */
@RunWith(MockitoJUnitRunner::class)
class BugsnagApiTest {

    @Mock
    lateinit var client: Client

    @Before
    fun setUp() {
        Bugsnag.client = client
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
    fun isStartedWhenStarted() {
        assertEquals(true, Bugsnag.isStarted())
    }

    @Test
    fun isStartedWhenNotStarted() {
        Bugsnag.client = null
        assertEquals(false, Bugsnag.isStarted())
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
    fun addOnError() {
        Bugsnag.addOnError { true }
        Bugsnag.addOnError(OnErrorCallback { true })
        verify(client, times(2)).addOnError(ArgumentMatchers.any())
    }

    @Test
    fun removeOnError() {
        Bugsnag.removeOnError { true }
        Bugsnag.removeOnError(OnErrorCallback { true })
        verify(client, times(2)).removeOnError(ArgumentMatchers.any())
    }

    @Test
    fun addOnBreadcrumb() {
        Bugsnag.addOnBreadcrumb { true }
        Bugsnag.addOnBreadcrumb(OnBreadcrumbCallback { true })
        verify(client, times(2)).addOnBreadcrumb(ArgumentMatchers.any())
    }

    @Test
    fun removeOnBreadcrumb() {
        Bugsnag.removeOnBreadcrumb { true }
        Bugsnag.removeOnBreadcrumb(OnBreadcrumbCallback { true })
        verify(client, times(2)).removeOnBreadcrumb(ArgumentMatchers.any())
    }

    @Test
    fun addOnSession() {
        Bugsnag.addOnSession { true }
        Bugsnag.addOnSession(OnSessionCallback { true })
        verify(client, times(2)).addOnSession(ArgumentMatchers.any())
    }

    @Test
    fun removeOnSession() {
        Bugsnag.removeOnSession { true }
        Bugsnag.removeOnSession(OnSessionCallback { true })
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
        val onError = OnErrorCallback { true }
        Bugsnag.notify(exc, onError)
        verify(client, times(1)).notify(exc, onError)
    }

    @Test
    fun addMetadataTopLevel() {
        val map = mutableMapOf(Pair("bar", "wham"))
        Bugsnag.addMetadata("foo", map)
        verify(client, times(1)).addMetadata("foo", map)
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
        Bugsnag.leaveBreadcrumb("whoops", mapOf(), BreadcrumbType.LOG)
        verify(client, times(1)).leaveBreadcrumb("whoops", mapOf(), BreadcrumbType.LOG)
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

    @Test
    fun getBreadcrumbs() {
        Bugsnag.getBreadcrumbs()
        verify(client, times(1)).breadcrumbs
    }

    @Test(expected = IllegalStateException::class)
    fun nullClient() {
        Bugsnag.client = null
        Bugsnag.getClient()
    }

    @Test
    fun lastRunInfo() {
        Bugsnag.getLastRunInfo()
        verify(client, times(1)).getLastRunInfo()
    }

    @Test
    fun markLaunchCompleted() {
        Bugsnag.markLaunchCompleted()
        verify(client, times(1)).markLaunchCompleted()
    }
}
