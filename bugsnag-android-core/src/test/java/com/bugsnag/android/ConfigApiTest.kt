package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

/**
 * Verifies that method calls are forwarded onto the appropriate method on Configuration
 * subclasses, and that adequate sanitisation takes place.
 */
@RunWith(MockitoJUnitRunner::class)
internal class ConfigApiTest {

    lateinit var config: Configuration
    lateinit var callbackState: CallbackState
    lateinit var metadataState: MetadataState

    @Before
    fun setUp() {
        config = Configuration("api-key")
        callbackState = config.callbackState
        metadataState = config.metadataState
    }

    @Test
    fun addOnError() {
        val onError = OnError { true }
        config.addOnError(onError)
        assertTrue(callbackState.onErrorTasks.contains(onError))
    }

    @Test
    fun removeOnError() {
        val onError = OnError { true }
        config.addOnError(onError)
        config.removeOnError(onError)
        assertFalse(callbackState.onErrorTasks.contains(onError))
    }

    @Test
    fun addOnBreadcrumb() {
        val onBreadcrumb = OnBreadcrumb { true }
        config.addOnBreadcrumb(onBreadcrumb)
        assertTrue(callbackState.onBreadcrumbTasks.contains(onBreadcrumb))
    }

    @Test
    fun removeOnBreadcrumb() {
        val onBreadcrumb = OnBreadcrumb { true }
        config.addOnBreadcrumb(onBreadcrumb)
        config.removeOnBreadcrumb(onBreadcrumb)
        assertFalse(callbackState.onBreadcrumbTasks.contains(onBreadcrumb))
    }

    @Test
    fun addOnSession() {
        val onSession = OnSession { true }
        config.addOnSession(onSession)
        assertTrue(callbackState.onSessionTasks.contains(onSession))
    }

    @Test
    fun removeOnSession() {
        val onSession = OnSession { true }
        config.addOnSession(onSession)
        config.removeOnSession(onSession)
        assertFalse(callbackState.onSessionTasks.contains(onSession))
    }

    @Test
    fun addMetadata() {
        config.addMetadata("foo", "bar")
        assertEquals("bar", metadataState.getMetadata("foo"))
    }

    @Test
    fun addMetadata1() {
        config.addMetadata("foo", "bar", "wham")
        assertEquals("wham", metadataState.getMetadata("foo", "bar"))
    }

    @Test
    fun clearMetadata() {
        metadataState.addMetadata("foo", "bar")
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
