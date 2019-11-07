package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.convert
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
internal class NotifyDelegateTest {

    @Mock
    lateinit var appData: AppData

    @Mock
    lateinit var deviceData: DeviceData

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var sessionTracker: SessionTracker

    lateinit var delegate: NotifyDelegate

    lateinit var user: User
    lateinit var contextState: ContextState
    lateinit var session: Session
    lateinit var breadcrumbState: BreadcrumbState
    lateinit var metadataState: MetadataState
    lateinit var callbackState: CallbackState

    @Before
    fun setUp() {
        user = User("123", "j@b.com", "JD")
        contextState = ContextState("my_context")
        session = Session("123", Date(), user, 1, 0)
        breadcrumbState = BreadcrumbState(50, NoopLogger)
        breadcrumbState.add(Breadcrumb("Whoops"))
        metadataState = MetadataState()
        metadataState.addMetadata("Foo", "Bar")
        callbackState = CallbackState()

        `when`(userRepository.load()).thenReturn(user)
        `when`(sessionTracker.currentSession).thenReturn(session)
        `when`(deviceData.deviceData).thenReturn(mapOf(Pair("id", "44")))
        `when`(deviceData.deviceMetadata).thenReturn(mapOf(Pair("meta", "foo")))
        `when`(appData.appData).thenReturn(mapOf(Pair("app", "Photosnap")))
        `when`(appData.appDataMetadata).thenReturn(mapOf(Pair("meta", "bar")))
        delegate = setupDelegate(generateImmutableConfig())
    }

    @Test
    fun notifyExc() {
        val exc = RuntimeException("Whoops")
        val event = delegate.notify(exc, null)

        assertEquals(Severity.WARNING, event.severity)
        assertFalse(event.unhandled)
        assertEquals(exc, event.originalError)
        assertEquals("java.lang.RuntimeException", event.errors[0].errorClass)
        assertEquals("Whoops", event.errors[0].errorMessage)

        val stacktrace = event.errors[0].stacktrace
        assertEquals("com.bugsnag.android.NotifyDelegateTest.notifyExc", stacktrace[0].method)
        val threadTrace = event.threads.single { it.isErrorReportingThread }
        assertTrue(stacktrace[0].method == threadTrace.stacktrace[0].method)
        validateEventMetadata(event)
    }

    @Test
    fun notifyByMessage() {
        val exc = RuntimeException("Whoops")
        val event = delegate.notify("LeakException", "Whoops", exc.stackTrace, null)

        assertEquals(Severity.WARNING, event.severity)
        assertFalse(event.unhandled)
        assertNull(event.originalError)
        assertEquals("LeakException", event.errors[0].errorClass)
        assertEquals("Whoops", event.errors[0].errorMessage)

        val stacktrace = event.errors[0].stacktrace
        assertEquals("com.bugsnag.android.NotifyDelegateTest.notifyByMessage", stacktrace[0].method)
        val threadTrace = event.threads.single { it.isErrorReportingThread }
        assertTrue(stacktrace[0].method == threadTrace.stacktrace[0].method)
        validateEventMetadata(event)
    }

    @Test
    fun notifyUnhandledException() {
        val exc = RuntimeException("Whoops")
        val event = delegate.notifyUnhandledException(
            exc,
            HandledState.REASON_UNHANDLED_EXCEPTION,
            null,
            java.lang.Thread.currentThread(),
            null
        )

        assertEquals(Severity.ERROR, event.severity)
        assertTrue(event.unhandled)
        assertEquals(exc, event.originalError)
        assertEquals("java.lang.RuntimeException", event.errors[0].errorClass)
        assertEquals("Whoops", event.errors[0].errorMessage)

        val stacktrace = event.errors[0].stacktrace
        assertEquals(
            "com.bugsnag.android.NotifyDelegateTest.notifyUnhandledException",
            stacktrace[0].method
        )
        val threadTrace = event.threads.single { it.isErrorReportingThread }
        assertTrue(stacktrace[0].method == threadTrace.stacktrace[0].method)
        validateEventMetadata(event)
    }

    @Test
    fun ignoreClassReturnsNull() {
        val config = generateConfiguration()
        config.ignoreClasses = setOf("java.lang.RuntimeException")
        delegate = setupDelegate(convert(config))
        assertNull(delegate.notify(java.lang.RuntimeException(), null))
    }

    @Test
    fun releaseStageReturnsNull() {
        val config = generateConfiguration()
        config.releaseStage = "beta"
        config.enabledReleaseStages = setOf("alpha")
        delegate = setupDelegate(convert(config))
        assertNull(delegate.notify(java.lang.RuntimeException(), null))
    }

    @Test
    fun localOnErrorReturnsFalse() {
        delegate = setupDelegate(generateImmutableConfig())
        assertNull(delegate.notify(java.lang.RuntimeException()) {
            false
        })
    }

    @Test
    fun globalOnErrorReturnsFalse() {
        callbackState.addOnError(OnError { false })
        assertNull(delegate.notify(java.lang.RuntimeException(), null))
    }

    private fun validateEventMetadata(event: Event) {
        assertEquals(session, event.session)

        assertEquals(mapOf(Pair("id", "44")), event.device)
        assertEquals(mapOf(Pair("meta", "foo")), event.getMetadata("device"))

        assertEquals(mapOf(Pair("app", "Photosnap")), event.app)
        assertEquals(mapOf(Pair("meta", "bar")), event.getMetadata("app"))

        assertEquals(breadcrumbState.store.toList(), event.breadcrumbs)
        assertEquals("Bar", event.getMetadata("Foo"))

        assertEquals(user, event.getUser())
        assertEquals(contextState.context, event.context)
        assertNull(event.groupingHash)
    }

    private fun setupDelegate(immutableConfig: ImmutableConfig) = NotifyDelegate(
        immutableConfig,
        metadataState,
        UserState(userRepository),
        contextState,
        breadcrumbState,
        callbackState,
        appData,
        deviceData,
        sessionTracker,
        NoopLogger
    )
}
