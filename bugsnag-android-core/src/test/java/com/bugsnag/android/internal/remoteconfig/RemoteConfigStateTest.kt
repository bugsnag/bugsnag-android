package com.bugsnag.android.internal.remoteconfig

import com.bugsnag.android.NoopLogger
import com.bugsnag.android.Notifier
import com.bugsnag.android.RemoteConfig
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.net.InetAddress
import java.net.ServerSocket
import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

class RemoteConfigStateTest {

    @get:Rule
    val tempDir = TemporaryFolder()

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
        // Given - a valid config already in memory that does not need refresh
        val validConfig = createValidRemoteConfig(
            "in-memory",
            futureDate(RemoteConfigState.REFRESH_BUFFER_MS + 100000L)
        )
        `when`(mockStore.current()).thenReturn(validConfig)

        // When - getRemoteConfig is called with timeout
        val result = remoteConfigState.getRemoteConfig(100L, TimeUnit.MILLISECONDS)

        // Then - should return the in-memory config immediately
        assertEquals("in-memory", result?.configurationTag)

        // Verify store.current() was called but no background task was needed
        verify(mockStore).current()
        verify(mockBackgroundTaskService, never()).submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java))
    }

    @Test
    fun getRemoteConfigReturnsInMemoryConfigAndSchedulesRefreshWhenNearExpiry() {
        // Given - a valid config in memory that is near expiry
        val nearExpiryConfig = createValidRemoteConfig("near-expiry", futureDate(1000))
        `when`(mockStore.current()).thenReturn(nearExpiryConfig)
        `when`(mockStore.currentOrExpired()).thenReturn(nearExpiryConfig)
        `when`(mockBackgroundTaskService.submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java)))
            .thenReturn(mockFuture)

        // When - getRemoteConfig is called
        val result = remoteConfigState.getRemoteConfig(100L, TimeUnit.MILLISECONDS)

        // Then - should return the in-memory config immediately
        assertEquals("near-expiry", result?.configurationTag)

        // Verify that a background refresh task was scheduled
        verify(mockBackgroundTaskService).submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java))
    }

    @Test
    fun getRemoteConfigSchedulesUpdateWhenConfigExpired() {
        // Given - no config in memory, but one exists in store that's near expiry
        val nearExpiryConfig = createValidRemoteConfig("near-expiry", futureDate(1000))
        `when`(mockStore.current()).thenReturn(null)
        `when`(mockStore.load()).thenReturn(nearExpiryConfig)

        // Mock background task service to return our future
        `when`(mockBackgroundTaskService.submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java)))
            .thenReturn(mockFuture)
        `when`(mockFuture.get(anyLong(), any())).thenReturn(nearExpiryConfig)

        // When - getRemoteConfig is called
        val result = remoteConfigState.getRemoteConfig(100L, TimeUnit.MILLISECONDS)

        // Then - should return the config from the background task
        assertEquals("near-expiry", result?.configurationTag)

        // Verify that a background task was submitted
        verify(mockBackgroundTaskService).submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java))
    }

    @Test
    fun getRemoteConfigSchedulesDownloadWhenNoConfigAvailable() {
        // Given - no config available anywhere
        `when`(mockStore.current()).thenReturn(null)
        `when`(mockStore.load()).thenReturn(null)
        `when`(mockStore.currentOrExpired()).thenReturn(null)

        // Mock background task to return null (no config available)
        `when`(mockBackgroundTaskService.submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java)))
            .thenReturn(mockFuture)
        `when`(mockFuture.get(100L, TimeUnit.MILLISECONDS)).thenReturn(null)

        // When - getRemoteConfig is called
        val result = remoteConfigState.getRemoteConfig(100L, TimeUnit.MILLISECONDS)

        // Then - should return null and have attempted to load/download
        assertNull(result)

        // Verify that a background task was submitted for loading/downloading
        verify(mockBackgroundTaskService).submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java))
    }

    @Test
    fun getRemoteConfigReturnsNullWhenIOTakesTooLongAndNoCachedConfig() {
        // Given - no config in memory and store operation takes too long, no fallback available
        `when`(mockStore.current()).thenReturn(null)
        `when`(mockStore.currentOrExpired()).thenReturn(null)

        // Mock background task service to return future that times out
        `when`(mockBackgroundTaskService.submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java)))
            .thenReturn(mockFuture)
        `when`(mockFuture.get(anyLong(), any()))
            .thenThrow(TimeoutException("Operation timed out"))

        // When - getRemoteConfig is called with 100ms timeout
        val result = remoteConfigState.getRemoteConfig(100L, TimeUnit.MILLISECONDS)

        // Then - should return null due to timeout and no cached fallback
        assertNull(result)

        // Verify that the timeout was respected
        verify(mockFuture).get(100L, TimeUnit.MILLISECONDS)
    }

    @Test
    fun getRemoteConfigDoesNotReturnExpiredFallbackWhenIOTakesTooLong() {
        // Given - store operation takes too long and only an expired cached config exists
        `when`(mockStore.current()).thenReturn(null)

        `when`(mockBackgroundTaskService.submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java)))
            .thenReturn(mockFuture)
        `when`(mockFuture.get(anyLong(), any()))
            .thenThrow(TimeoutException("Operation timed out"))

        // When - getRemoteConfig is called with 100ms timeout
        val result = remoteConfigState.getRemoteConfig(100L, TimeUnit.MILLISECONDS)

        // Then - expired rules must not be returned to delivery
        assertNull(result)
    }

    @Test
    fun getRemoteConfigDoesNotReturnExpiredRequestResult() {
        val expiredConfig = createValidRemoteConfig("expired", futureDate(-1000))
        `when`(mockStore.current()).thenReturn(null)
        `when`(mockBackgroundTaskService.submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java)))
            .thenReturn(mockFuture)
        `when`(mockFuture.get(100L, TimeUnit.MILLISECONDS)).thenReturn(expiredConfig)

        val result = remoteConfigState.getRemoteConfig(100L, TimeUnit.MILLISECONDS)

        assertNull(result)
    }

    @Test
    fun peekRemoteConfigReturnsOnlyValidConfig() {
        val validConfig = createValidRemoteConfig("valid", futureDate(1000))
        `when`(mockStore.current()).thenReturn(validConfig)

        val result = remoteConfigState.peekRemoteConfig()

        assertEquals("valid", result?.configurationTag)
        verify(mockStore).current()
        verify(mockStore, never()).currentOrExpired()
    }

    @Test
    fun getRemoteConfigReusesInFlightRequest() {
        // Given - the first remote-config request is still running
        `when`(mockStore.current()).thenReturn(null)
        `when`(mockStore.load()).thenReturn(null)
        `when`(mockStore.currentOrExpired()).thenReturn(null)
        `when`(mockBackgroundTaskService.submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java)))
            .thenReturn(mockFuture)

        // When - we ask for remote config twice before the first request finishes
        val firstRequest = remoteConfigState.getRemoteConfig()
        val secondRequest = remoteConfigState.getRemoteConfig()

        // Then - both calls should return the same in-flight request
        assertEquals(firstRequest, secondRequest)
        verify(mockBackgroundTaskService).submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java))
    }

    @Test
    fun getRemoteConfigReturnsNullWhenDisabled() {
        // Given - remote config is disabled (no configuration endpoint)
        val mockEndpoints = mock(com.bugsnag.android.EndpointConfiguration::class.java)
        `when`(mockEndpoints.configuration).thenReturn(null)
        `when`(mockConfig.endpoints).thenReturn(mockEndpoints)

        // a separate service mock, as the enabled state built in setup() has already scheduled a sweep
        val disabledTaskService = mock(BackgroundTaskService::class.java)
        val disabledState = RemoteConfigState(mockStore, mockConfig, mockNotifier, disabledTaskService)

        verify(mockStore).clear()

        // When - getRemoteConfig is called
        val result = disabledState.getRemoteConfig(100L, TimeUnit.MILLISECONDS)

        // Then - should return null immediately
        assertNull(result)

        // Verify no additional interactions with store or background service
        verifyNoMoreInteractions(mockStore)
        verifyNoInteractions(disabledTaskService) // no sweep or fetch when disabled
    }

    @Test
    fun scheduleDownloadIfRequiredRefreshesNearExpiryConfig() {
        val nearExpiryConfig = createValidRemoteConfig(
            "near-expiry",
            futureDate(RemoteConfigState.REFRESH_BUFFER_MS - 1000)
        )
        `when`(mockStore.current()).thenReturn(nearExpiryConfig)
        `when`(mockBackgroundTaskService.submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java)))
            .thenReturn(mockFuture)

        remoteConfigState.scheduleDownloadIfRequired()

        verify(mockBackgroundTaskService).submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java))
    }

    @Test
    fun scheduleDownloadIfRequiredSkipsRequestDuringCooldown() {
        val nearExpiryConfig = createValidRemoteConfig("near-expiry", futureDate(1000))
        `when`(mockStore.current()).thenReturn(nearExpiryConfig)
        `when`(mockStore.cachedCooldownUntil()).thenReturn(System.currentTimeMillis() + 60_000)

        remoteConfigState.scheduleDownloadIfRequired()

        verify(mockBackgroundTaskService, never()).submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java))
    }

    @Test
    fun scheduleDownloadIfRequiredRefreshesWhenCooldownMarkerHasExpired() {
        val nearExpiryConfig = createValidRemoteConfig("near-expiry", futureDate(1000))
        `when`(mockStore.current()).thenReturn(nearExpiryConfig)
        `when`(mockStore.cachedCooldownUntil()).thenReturn(System.currentTimeMillis() - 1)
        `when`(mockBackgroundTaskService.submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java)))
            .thenReturn(mockFuture)

        remoteConfigState.scheduleDownloadIfRequired()

        verify(mockBackgroundTaskService).submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java))
    }

    @Test
    fun scheduleDownloadIfRequiredDoesNotReadDiskOnCallingThread() {
        `when`(mockStore.current()).thenReturn(null)
        `when`(mockStore.cachedCooldownUntil()).thenReturn(0L)
        `when`(mockBackgroundTaskService.submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java)))
            .thenReturn(mockFuture)

        remoteConfigState.scheduleDownloadIfRequired()

        verify(mockBackgroundTaskService).submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java))
        verify(mockStore, never()).currentOrExpired()
        verify(mockStore, never()).cooldownUntil()
        verify(mockStore, never()).diagnostics()
    }

    @Test
    fun scheduleDownloadIfRequiredSkipsRequestWhenOffline() {
        `when`(mockStore.current()).thenReturn(null)
        `when`(mockStore.cachedCooldownUntil()).thenReturn(0L)
        val offlineState = RemoteConfigState(
            mockStore,
            mockConfig,
            mockNotifier,
            mockBackgroundTaskService,
            networkAvailable = { false }
        )

        offlineState.scheduleDownloadIfRequired()

        verify(mockBackgroundTaskService, never()).submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java))
    }

    @Test
    fun requestIsNotSentAndBackoffNotStartedWhenConnectionDropsBeforeRequest() {
        val networkChecks = AtomicInteger()
        // online when scheduled, offline by the time the background task runs
        val (state, store, taskService) = stateWithRealStore { networkChecks.incrementAndGet() == 1 }

        runScheduledDownload(state, taskService)

        assertEquals(0L, store.cooldownUntil())
        assertEquals(0, store.failureCount())
    }

    @Test
    fun failedRequestDoesNotStartBackoffWhenConnectionWasLost() {
        val networkChecks = AtomicInteger()
        // online when scheduled and when the request starts, offline once it has failed
        val (state, store, taskService) = stateWithRealStore { networkChecks.incrementAndGet() <= 2 }

        runScheduledDownload(state, taskService)

        assertEquals(3, networkChecks.get())
        assertEquals(0L, store.cooldownUntil())
        assertEquals(0, store.failureCount())
    }

    @Test
    fun failedRequestDoesNotStartBackoffWhenNetworkLossIsReportedLate() {
        // Seen on a Galaxy A12: the socket error arrives before Android reports Wi-Fi as gone, so
        // the device still looks online straight after the failure and only offline a moment later
        val networkChecks = AtomicInteger()
        val (state, store, taskService) = stateWithRealStore { networkChecks.incrementAndGet() <= 3 }

        runScheduledDownload(state, taskService)

        assertEquals("the failure is re-checked once after the grace period", 4, networkChecks.get())
        assertEquals(0L, store.cooldownUntil())
        assertEquals(0, store.failureCount())
    }

    @Test
    fun httpErrorStartsBackoffEvenIfConnectionIsLostAfterwards() {
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val responder = Thread {
            runCatching {
                server.accept().use { socket ->
                    // read the request headers before answering so the client sees the response
                    val input = socket.getInputStream().bufferedReader()
                    while (!input.readLine().isNullOrEmpty()) Unit
                    socket.getOutputStream().write(
                        "HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                            .toByteArray()
                    )
                    socket.getOutputStream().flush()
                }
            }
        }.apply { start() }
        try {
            val networkChecks = AtomicInteger()
            // online until the response arrives, offline from then on
            val (state, store, taskService) = stateWithRealStore(
                endpoint = "http://127.0.0.1:${server.localPort}/"
            ) { networkChecks.incrementAndGet() <= 2 }

            runScheduledDownload(state, taskService)

            // the service answered, so going offline afterwards does not excuse the failure
            assertTrue(store.cooldownUntil() > System.currentTimeMillis())
            assertEquals(1, store.failureCount())
        } finally {
            server.close()
            responder.join(1000)
        }
    }

    @Test
    fun failedRequestStartsBackoffWhenOnline() {
        val (state, store, taskService) = stateWithRealStore { true }

        runScheduledDownload(state, taskService)

        assertTrue(store.cooldownUntil() > System.currentTimeMillis())
        assertEquals(1, store.failureCount())
    }

    /**
     * A state backed by a real store. By default its requests go to a closed local port so they
     * fail straight away without leaving the machine.
     */
    private fun stateWithRealStore(
        endpoint: String = "http://127.0.0.1:1/",
        networkAvailable: () -> Boolean
    ): Triple<RemoteConfigState, RemoteConfigStore, BackgroundTaskService> {
        val endpoints = mock(com.bugsnag.android.EndpointConfiguration::class.java)
        `when`(endpoints.configuration).thenReturn(endpoint)
        val config = mock(ImmutableConfig::class.java)
        `when`(config.endpoints).thenReturn(endpoints)
        `when`(config.apiKey).thenReturn("5d1ec5bd39a74caa1267142706a7fb21")
        `when`(config.logger).thenReturn(NoopLogger)
        `when`(mockNotifier.name).thenReturn("Android Bugsnag Notifier")
        `when`(mockNotifier.version).thenReturn("0.0.0")

        val configDir = tempDir.newFolder("config")
        val store = RemoteConfigStore(configDir, 1, NoopLogger, cooldownDir = configDir)
        val taskService = mock(BackgroundTaskService::class.java)
        `when`(taskService.submitTask(eq(TaskType.REMOTE_CONFIG), any(Callable::class.java)))
            .thenReturn(mockFuture)
        val state = RemoteConfigState(
            store,
            config,
            mockNotifier,
            taskService,
            networkAvailable = networkAvailable,
            networkLossGraceMs = 10
        )
        return Triple(state, store, taskService)
    }

    @Suppress("UNCHECKED_CAST")
    private fun runScheduledDownload(state: RemoteConfigState, taskService: BackgroundTaskService) {
        state.scheduleDownloadIfRequired()
        val task = ArgumentCaptor.forClass(Callable::class.java)
        verify(taskService).submitTask(eq(TaskType.REMOTE_CONFIG), task.capture())
        (task.value as Callable<RemoteConfig?>).call()
    }

    @Test
    fun sweepsStaleVersionsInBackgroundAtStartup() {
        val runnable = ArgumentCaptor.forClass(Runnable::class.java)
        verify(mockBackgroundTaskService).submitTask(eq(TaskType.REMOTE_CONFIG), runnable.capture())
        verify(mockStore, never()).sweep()

        runnable.value.run()

        verify(mockStore).sweep()
    }

    @Test
    fun backoffCeilingGrowsExponentiallyUpToCap() {
        val minutes = TimeUnit.MINUTES::toMillis
        assertEquals(minutes(15), RemoteConfigState.backoffCeiling(0))
        assertEquals(minutes(60), RemoteConfigState.backoffCeiling(1))
        assertEquals(minutes(240), RemoteConfigState.backoffCeiling(2))
        assertEquals(minutes(960), RemoteConfigState.backoffCeiling(3))
        assertEquals(TimeUnit.HOURS.toMillis(24), RemoteConfigState.backoffCeiling(4))
        assertEquals(TimeUnit.HOURS.toMillis(24), RemoteConfigState.backoffCeiling(5))
        assertEquals(TimeUnit.HOURS.toMillis(24), RemoteConfigState.backoffCeiling(Int.MAX_VALUE))
        assertEquals(minutes(15), RemoteConfigState.backoffCeiling(-1))
    }

    @Test
    fun computeCooldownAppliesEqualJitter() {
        val nowMs = 1234L
        val ceilingMs = TimeUnit.MINUTES.toMillis(60)

        val floor = RemoteConfigState.computeCooldown(failureIndex = 1, jitterMs = 0L, nowMs = nowMs)
        assertEquals(ceilingMs, floor.ceilingMs)
        assertEquals(ceilingMs / 2, floor.durationMs)
        assertEquals(nowMs + ceilingMs / 2, floor.cooldownUntilMs)

        val ceiling = RemoteConfigState.computeCooldown(failureIndex = 1, jitterMs = ceilingMs, nowMs = nowMs)
        assertEquals(ceilingMs, ceiling.durationMs)
        assertEquals(nowMs + ceilingMs, ceiling.cooldownUntilMs)
    }

    @Test
    fun computeCooldownRandomWaitStaysWithinFleetWindow() {
        for (k in 0..6) {
            val ceilingMs = RemoteConfigState.backoffCeiling(k)
            repeat(200) {
                val wait = RemoteConfigState.computeCooldown(k, nowMs = 0L).durationMs
                assertTrue("k=$k wait=$wait", wait >= ceilingMs / 2 && wait <= ceilingMs)
            }
        }
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
