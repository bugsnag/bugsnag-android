package com.bugsnag.android

import android.os.strictmode.FakeStrictModeViolation
import android.os.strictmode.Violation
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

/**
 * Asserts that the chained listener is called if it has been set.
 */
@RunWith(MockitoJUnitRunner::class)
class StrictModeChainedListenerTest {

    @Mock
    lateinit var client: Client

    @Test
    fun testChainedThreadListener() {
        var violation: Violation? = null
        val listener = BugsnagThreadViolationListener(client) {
            violation = it
        }
        val expected = FakeStrictModeViolation()
        listener.onThreadViolation(expected)
        assertSame(expected, violation)
    }

    @Test
    fun testChainedVmListener() {
        var violation: Violation? = null
        val listener = BugsnagVmViolationListener(client) {
            violation = it
        }
        val expected = FakeStrictModeViolation()
        listener.onVmViolation(expected)
        assertSame(expected, violation)
    }
}
