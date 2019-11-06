package com.bugsnag.android

import android.content.Context
import android.content.SharedPreferences
import android.view.OrientationEventListener
import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

/**
 * Verifies that method calls are forwarded onto the appropriate method on Client subclasses,
 * and that adequate sanitisation takes place.
 */
@RunWith(MockitoJUnitRunner::class)
internal class ClientApiTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var appData: AppData

    @Mock
    lateinit var deviceData: DeviceData

    @Mock
    lateinit var eventStore: EventStore

    @Mock
    lateinit var sessionStore: SessionStore

    @Mock
    lateinit var systemBroadcastReceiver: SystemBroadcastReceiver

    @Mock
    lateinit var sessionTracker: SessionTracker

    @Mock
    lateinit var sharedPreferences: SharedPreferences

    @Mock
    lateinit var orientationEventListener: OrientationEventListener

    @Mock
    lateinit var connectivity: Connectivity

    @Mock
    lateinit var reportDeliveryDelegate: ReportDeliveryDelegate

    @Mock
    lateinit var notifyDelegate: NotifyDelegate

    lateinit var client: Client

    lateinit var callbackState: CallbackState
    lateinit var metadataState: MetadataState
    lateinit var contextState: ContextState
    lateinit var breadcrumbState: BreadcrumbState
    lateinit var userState: UserState

    @Before
    fun setUp() {
        `when`(userRepository.load()).thenReturn(User("123", "joe@yahoo.com", "Joe"))

        callbackState = CallbackState()
        metadataState = MetadataState()
        contextState = ContextState()
        breadcrumbState = BreadcrumbState(50, NoopLogger)
        userState = UserState(userRepository)

        client = Client(
            generateImmutableConfig(),
            callbackState,
            metadataState,
            contextState,
            userState,
            context,
            deviceData,
            appData,
            breadcrumbState,
            eventStore,
            sessionStore, systemBroadcastReceiver,
            sessionTracker,
            sharedPreferences,
            orientationEventListener,
            connectivity,
            userRepository,
            NoopLogger,
            reportDeliveryDelegate,
            notifyDelegate
        )
    }

    @Test
    fun startSession() {
        client.startSession()
        verify(sessionTracker, times(1)).startSession(false)
    }

    @Test
    fun pauseSession() {
        client.pauseSession()
        verify(sessionTracker, times(1)).pauseSession()
    }

    @Test
    fun resumeSession() {
        client.resumeSession()
        verify(sessionTracker, times(1)).resumeSession()
    }

    @Test
    fun getContext() {
        contextState.context = "foo"
        assertEquals("foo", client.context)
    }

    @Test
    fun setContext() {
        client.context = "bar"
        assertEquals("bar", contextState.context)
    }

    @Test
    fun setUser() {
        client.setUser("4", "t@d.com", "Mr T")
        assertEquals(User("4", "t@d.com", "Mr T"), client.getUser())
    }

    @Test
    fun getUser() {
        assertEquals(User("123", "joe@yahoo.com", "Joe"), client.getUser())
    }

    @Test
    fun setUserId() {
        client.setUserId("999")
        assertEquals(User("999", "joe@yahoo.com", "Joe"), client.getUser())
    }

    @Test
    fun setUserEmail() {
        client.setUserEmail("jill@tiscali.com")
        assertEquals(User("123", "jill@tiscali.com", "Joe"), client.getUser())
    }

    @Test
    fun setUserName() {
        client.setUserName("Jane")
        assertEquals(User("123", "joe@yahoo.com", "Jane"), client.getUser())
    }

    @Test
    fun addOnError() {
        val onError = OnError { true }
        client.addOnError(onError)
        assertTrue(callbackState.onErrorTasks.contains(onError))
    }

    @Test
    fun removeOnError() {
        val onError = OnError { true }
        client.addOnError(onError)
        client.removeOnError(onError)
        assertFalse(callbackState.onErrorTasks.contains(onError))
    }

    @Test
    fun addOnBreadcrumb() {
        val onBreadcrumb = OnBreadcrumb { true }
        client.addOnBreadcrumb(onBreadcrumb)
        assertTrue(callbackState.onBreadcrumbTasks.contains(onBreadcrumb))
    }

    @Test
    fun removeOnBreadcrumb() {
        val onBreadcrumb = OnBreadcrumb { true }
        client.addOnBreadcrumb(onBreadcrumb)
        client.removeOnBreadcrumb(onBreadcrumb)
        assertFalse(callbackState.onBreadcrumbTasks.contains(onBreadcrumb))
    }

    @Test
    fun addOnSession() {
        val onSession = OnSession { true }
        client.addOnSession(onSession)
        assertTrue(callbackState.onSessionTasks.contains(onSession))
    }

    @Test
    fun removeOnSession() {
        val onSession = OnSession { true }
        client.addOnSession(onSession)
        client.removeOnSession(onSession)
        assertFalse(callbackState.onSessionTasks.contains(onSession))
    }

    @Test
    fun notify1() {
        val exc = RuntimeException()
        client.notify(exc)
        verify(notifyDelegate, times(1)).notify(exc, null)
    }

    @Test
    fun notify2() {
        val exc = RuntimeException()
        val onError = OnError { true }
        client.notify(exc, onError)
        verify(notifyDelegate, times(1)).notify(exc, onError)
    }

    @Test
    fun notify3() {
        client.notify("Leak", "whoops", arrayOf())
        verify(notifyDelegate, times(1)).notify("Leak", "whoops", arrayOf(), null)
    }

    @Test
    fun notify4() {
        val onError = OnError { true }
        client.notify("Leak", "whoops", arrayOf(), onError)
        verify(notifyDelegate, times(1)).notify("Leak", "whoops", arrayOf(), onError)
    }

    @Test
    fun addMetadata() {
        client.addMetadata("foo", "bar")
        assertEquals("bar", metadataState.getMetadata("foo"))
    }

    @Test
    fun addMetadata1() {
        client.addMetadata("foo", "bar", "wham")
        assertEquals("wham", metadataState.getMetadata("foo", "bar"))
    }

    @Test
    fun clearMetadata() {
        metadataState.addMetadata("foo", "bar")
        client.clearMetadata("foo")
        assertNull( client.getMetadata("foo"))
    }

    @Test
    fun clearMetadata1() {
        metadataState.addMetadata("foo", "bar", "wham")
        client.clearMetadata("foo", "bar")
        assertNull(client.getMetadata("foo", "bar"))
    }

    @Test
    fun getMetadata() {
        metadataState.addMetadata("foo", "bar")
        assertEquals("bar", client.getMetadata("foo"))
    }

    @Test
    fun getMetadata1() {
        metadataState.addMetadata("foo", "bar", "wham")
        assertEquals("wham", client.getMetadata("foo", "bar"))
    }

    @Test
    fun leaveBreadcrumb() {
        client.leaveBreadcrumb("Whoops")
        assertEquals("Whoops", breadcrumbState.store.peek().metadata["message"])
    }

    @Test
    fun leaveBreadcrumb1() {
        client.leaveBreadcrumb("Whoops", BreadcrumbType.LOG, mapOf())
        assertEquals(BreadcrumbType.LOG, breadcrumbState.store.peek().type)
    }

    @Test
    fun setBinaryArch() {
        client.setBinaryArch("x86")
        verify(appData, times(1)).setBinaryArch("x86")
    }

    @Test
    fun close() {
        client.close()
        verify(orientationEventListener, times(1)).disable()
        verify(connectivity, times(1)).unregisterForNetworkChanges()
    }
}
