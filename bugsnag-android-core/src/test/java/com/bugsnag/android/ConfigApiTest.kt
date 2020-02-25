package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

/**
 * Verifies that method calls are forwarded onto the appropriate method on Configuration
 * subclasses, and that adequate sanitisation takes place.
 */
@RunWith(MockitoJUnitRunner::class)
internal class ConfigApiTest {

    lateinit var config: Configuration
    private lateinit var callbackState: CallbackState
    private lateinit var metadataState: MetadataState

    @Before
    fun setUp() {
        config = generateConfiguration()
        callbackState = config.impl.callbackState
        metadataState = config.impl.metadataState
    }

    @Test
    fun addOnError() {
        val onError = OnErrorCallback { true }
        config.addOnError(onError)
        assertTrue(callbackState.onErrorTasks.contains(onError))
    }

    @Test
    fun removeOnError() {
        val onError = OnErrorCallback { true }
        config.addOnError(onError)
        config.removeOnError(onError)
        assertFalse(callbackState.onErrorTasks.contains(onError))
    }

    @Test
    fun addOnBreadcrumb() {
        val onBreadcrumb = OnBreadcrumbCallback { true }
        config.addOnBreadcrumb(onBreadcrumb)
        assertTrue(callbackState.onBreadcrumbTasks.contains(onBreadcrumb))
    }

    @Test
    fun removeOnBreadcrumb() {
        val onBreadcrumb = OnBreadcrumbCallback { true }
        config.addOnBreadcrumb(onBreadcrumb)
        config.removeOnBreadcrumb(onBreadcrumb)
        assertFalse(callbackState.onBreadcrumbTasks.contains(onBreadcrumb))
    }

    @Test
    fun addOnSession() {
        val onSession = OnSessionCallback { true }
        config.addOnSession(onSession)
        assertTrue(callbackState.onSessionTasks.contains(onSession))
    }

    @Test
    fun removeOnSession() {
        val onSession = OnSessionCallback { true }
        config.addOnSession(onSession)
        config.removeOnSession(onSession)
        assertFalse(callbackState.onSessionTasks.contains(onSession))
    }

    @Test
    fun addMetadata() {
        config.addMetadata("foo", mapOf(Pair("wham", "bar")))
        assertEquals(mapOf(Pair("wham", "bar")), metadataState.getMetadata("foo"))
    }

    @Test
    fun addMetadata1() {
        config.addMetadata("foo", "bar", "wham")
        assertEquals("wham", metadataState.getMetadata("foo", "bar"))
    }

    @Test
    fun clearMetadata() {
        metadataState.addMetadata("foo", mapOf(Pair("wham", "bar")))
        config.clearMetadata("foo")
        assertNull(config.getMetadata("foo"))
    }

    @Test
    fun clearMetadata1() {
        metadataState.addMetadata("foo", "bar", "wham")
        config.clearMetadata("foo", "bar")
        assertNull(config.getMetadata("foo", "bar"))
    }
}
