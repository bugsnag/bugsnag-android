package com.bugsnag.android.internal.remoteconfig

import com.bugsnag.android.Notifier
import com.bugsnag.android.RemoteConfig
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class RemoteConfigStateTest {

    @Mock
    private lateinit var mockStore: RemoteConfigStore

    @Mock
    private lateinit var mockConfig: ImmutableConfig

    @Mock
    private lateinit var mockNotifier: Notifier

    @Mock
    private lateinit var mockBackgroundTaskService: BackgroundTaskService

    @Mock
    private lateinit var mockFuture: Future<RemoteConfig?>

    private lateinit var remoteConfigState: RemoteConfigState

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Mock the endpoints to be non-null (enabled)
        val mockEndpoints = mock(com.bugsnag.android.EndpointConfiguration::class.java)
        `when`(mockEndpoints.configuration).thenReturn("https://config.bugsnag.com")
        `when`(mockConfig.endpoints).thenReturn(mockEndpoints)

        remoteConfigState = RemoteConfigState(mockStore, mockConfig, mockNotifier, mockBackgroundTaskService)
    }

    @Test
    fun getRemoteConfigReturnsInMemoryConfig() {
        // Given - a valid config already in memory
        val validConfig = createValidRemoteConfig("in-memory", futureDate(5000))
        `when`(mockStore.current()).thenReturn(validConfig)

        // When - getRemoteConfig is called with timeout
        val result = remoteConfigState.getRemoteConfig(100L, TimeUnit.MILLISECONDS)

        // Then - should return the in-memory config immediately
        assertEquals("in-memory", result?.configurationTag)

        // Verify store.current() was called but no background task was needed
        verify(mockStore).current()
        verifyNoInteractions(mockBackgroundTaskService)
    }

    @Test
    fun getRemoteConfigSchedulesUpdateWhenConfigExpired() {
        // Given - no config in memory, but one exists in store that's near expiry
        val nearExpiryConfig = createValidRemoteConfig("near-expiry", futureDate(1000))
        `when`(mockStore.current()).thenReturn(null)
        `when`(mockStore.load()).thenReturn(nearExpiryConfig)

        // Mock background task service to return our future
        `when`(mockBackgroundTaskService.submitTask(eq(TaskType.IO), any(Callable::class.java)))
            .thenReturn(mockFuture)
        `when`(mockFuture.get(anyLong(), any())).thenReturn(nearExpiryConfig)

        // When - getRemoteConfig is called
        val result = remoteConfigState.getRemoteConfig(100L, TimeUnit.MILLISECONDS)

        // Then - should return the config from the background task
        assertEquals("near-expiry", result?.configurationTag)

        // Verify that a background task was submitted
        verify(mockBackgroundTaskService).submitTask(eq(TaskType.IO), any(Callable::class.java))
    }

    @Test
    fun getRemoteConfigSchedulesDownloadWhenNoConfigAvailable() {
        // Given - no config available anywhere
        `when`(mockStore.current()).thenReturn(null)
        `when`(mockStore.load()).thenReturn(null)
        `when`(mockStore.currentOrExpired()).thenReturn(null)

        // Mock background task to return null (no config available)
        `when`(mockBackgroundTaskService.submitTask(eq(TaskType.IO), any(Callable::class.java)))
            .thenReturn(mockFuture)
        `when`(mockFuture.get(100L, TimeUnit.MILLISECONDS)).thenReturn(null)

        // When - getRemoteConfig is called
        val result = remoteConfigState.getRemoteConfig(100L, TimeUnit.MILLISECONDS)

        // Then - should return null and have attempted to load/download
        assertNull(result)

        // Verify that a background task was submitted for loading/downloading
        verify(mockBackgroundTaskService).submitTask(eq(TaskType.IO), any(Callable::class.java))
    }

    @Test
    fun getRemoteConfigReturnsNullWhenIOTakesTooLong() {
        // Given - no config in memory and store operation takes too long
        `when`(mockStore.current()).thenReturn(null)

        // Mock background task service to return future that times out
        `when`(mockBackgroundTaskService.submitTask(eq(TaskType.IO), any(Callable::class.java)))
            .thenReturn(mockFuture)
        `when`(mockFuture.get(anyLong(), any()))
            .thenThrow(TimeoutException("Operation timed out"))

        // When - getRemoteConfig is called with 100ms timeout
        val result = remoteConfigState.getRemoteConfig(100L, TimeUnit.MILLISECONDS)

        // Then - should return null due to timeout
        assertNull(result)

        // Verify that the timeout was respected
        verify(mockFuture).get(100L, TimeUnit.MILLISECONDS)
    }

    @Test
    fun getRemoteConfigReturnsNullWhenDisabled() {
        // Given - remote config is disabled (no configuration endpoint)
        val mockEndpoints = mock(com.bugsnag.android.EndpointConfiguration::class.java)
        `when`(mockEndpoints.configuration).thenReturn(null)
        `when`(mockConfig.endpoints).thenReturn(mockEndpoints)

        val disabledState = RemoteConfigState(mockStore, mockConfig, mockNotifier, mockBackgroundTaskService)

        // When - getRemoteConfig is called
        val result = disabledState.getRemoteConfig(100L, TimeUnit.MILLISECONDS)

        // Then - should return null immediately
        assertNull(result)

        // Verify no interactions with store or background service
        verifyNoInteractions(mockStore)
        verifyNoInteractions(mockBackgroundTaskService)
    }

    private fun createValidRemoteConfig(tag: String, expiry: Date): RemoteConfig {
        return RemoteConfig(
            configurationTag = tag,
            configurationExpiry = expiry,
            discardRules = emptyList()
        )
    }

    private fun futureDate(offsetMs: Long): Date {
        return Date(System.currentTimeMillis() + offsetMs)
    }
}
