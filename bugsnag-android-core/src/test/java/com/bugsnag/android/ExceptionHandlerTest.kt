package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.lang.Thread

@RunWith(MockitoJUnitRunner::class)
internal class ExceptionHandlerTest {

    @Mock
    lateinit var client: Client

    @Mock
    lateinit var cfg: ImmutableConfig

    var originalHandler: Thread.UncaughtExceptionHandler? = null

    @Before
    fun setUp() {
        originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        `when`(client.config).thenReturn(cfg)
    }

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(originalHandler)
    }

    @Test
    fun handlerInstalled() {
        val exceptionHandler = ExceptionHandler(client, NoopLogger)
        assertSame(originalHandler, Thread.getDefaultUncaughtExceptionHandler())

        exceptionHandler.install()
        assertSame(exceptionHandler, Thread.getDefaultUncaughtExceptionHandler())

        exceptionHandler.uninstall()
        assertSame(originalHandler, Thread.getDefaultUncaughtExceptionHandler())
    }

    @Test
    fun uncaughtException() {
        val exceptionHandler = ExceptionHandler(client, NoopLogger)
        val thread = Thread.currentThread()
        val exc = RuntimeException("Whoops")
        exceptionHandler.uncaughtException(thread, exc)
        verify(client, times(1)).notifyUnhandledException(
            eq(exc),
            any(),
            eq(SeverityReason.REASON_UNHANDLED_EXCEPTION),
            eq(null)
        )
    }

    @Test
    fun exceptionPropagated() {
        var propagated = false
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> propagated = true }
        val exceptionHandler = ExceptionHandler(client, NoopLogger)
        val thread = Thread.currentThread()
        exceptionHandler.uncaughtException(thread, RuntimeException("Whoops"))
        assertTrue(propagated)
    }

    @Test
    fun exceptionPropagatedWhenDiscarded() {
        val runtimeException = RuntimeException("Whoops")
        `when`(cfg.shouldDiscardError(runtimeException)).thenReturn(true)

        var propagated = false
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> propagated = true }
        val exceptionHandler = ExceptionHandler(client, NoopLogger)
        val thread = Thread.currentThread()
        exceptionHandler.uncaughtException(thread, runtimeException)
        assertTrue(propagated)
    }

    @Test
    fun uncaughtExceptionOutsideReleaseStages() {
        val exceptionHandler = ExceptionHandler(client, NoopLogger)
        val thread = Thread.currentThread()
        val exc = RuntimeException("Whoops")
        `when`(cfg.shouldDiscardError(exc)).thenReturn(true)
        exceptionHandler.uncaughtException(thread, exc)
        verify(client, times(0)).notifyUnhandledException(
            eq(exc),
            any(),
            eq(SeverityReason.REASON_UNHANDLED_EXCEPTION),
            eq(null)
        )
    }
}
