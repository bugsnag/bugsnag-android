package com.bugsnag.android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.convertToImmutableConfig
import com.bugsnag.android.internal.dag.RunnableProvider
import com.bugsnag.android.internal.dag.ValueProvider
import com.bugsnag.android.internal.isInvalidApiKey
import com.bugsnag.android.internal.sanitiseConfiguration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.nio.file.Files
import java.util.regex.Pattern

@RunWith(MockitoJUnitRunner::class)
internal class ImmutableConfigTest {

    private val seed = generateConfiguration().apply {
        projectPackages = setOf("com.example.foo")
    }

    @Mock
    lateinit var delivery: Delivery

    @Mock
    lateinit var connectivity: Connectivity

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var packageManager: PackageManager

    lateinit var backgroundTaskService: BackgroundTaskService

    @Before
    fun setUp() {
        // these options are required, but are set in the Client constructor if no value is set
        // on the config object
        seed.delivery = delivery
        seed.logger = NoopLogger

        RunnableProvider._mainThread = java.lang.Thread.currentThread()

        // we use a real BackgroundTaskService
        backgroundTaskService = BackgroundTaskService()
    }

    @After
    fun shutdown() {
        // shutdown the backgroundTaskService to avoid leaking threads
        backgroundTaskService.shutdown()
    }

    @Test
    fun defaultConversion() {
        with(convertToImmutableConfig(seed)) {
            assertEquals("5d1ec5bd39a74caa1267142706a7fb21", apiKey)

            // detection
            assertTrue(autoTrackSessions)
            assertTrue(enabledErrorTypes.unhandledExceptions)
            assertTrue(enabledErrorTypes.anrs)
            assertTrue(enabledErrorTypes.ndkCrashes)
            assertEquals(ThreadSendPolicy.ALWAYS, sendThreads)

            // release stages
            assertTrue(discardClasses.isEmpty())
            assertNull(enabledReleaseStages)
            assertEquals(setOf("com.example.foo"), projectPackages)
            assertEquals(seed.releaseStage, releaseStage)

            // identifiers
            assertEquals(seed.appVersion, appVersion)
            assertNull(buildUuid)
            assertEquals(seed.appType, appType)

            // network config
            assertEquals(seed.delivery, delivery)
            assertEquals(seed.endpoints, endpoints)

            // behaviour
            assertEquals(seed.launchDurationMillis, launchDurationMillis)
            assertTrue(sendLaunchCrashesSynchronously)
            assertEquals(NoopLogger, seed.logger)
            assertEquals(seed.maxBreadcrumbs, maxBreadcrumbs)
            assertEquals(seed.maxPersistedEvents, maxPersistedEvents)
            assertEquals(seed.maxPersistedSessions, maxPersistedSessions)
            assertEquals(seed.persistUser, persistUser)
            assertNull(seed.enabledBreadcrumbTypes)
            assertNotNull(persistenceDirectory)
        }
    }

    @Test
    fun convertWithOverrides() {
        seed.autoTrackSessions = false
        seed.enabledErrorTypes.unhandledExceptions = false
        seed.enabledErrorTypes.anrs = false
        seed.enabledErrorTypes.ndkCrashes = false
        seed.sendThreads = ThreadSendPolicy.UNHANDLED_ONLY

        val discardClasses = setOf(Pattern.compile("foo"))
        seed.discardClasses = discardClasses
        seed.enabledReleaseStages = setOf("bar")
        seed.projectPackages = setOf("com.example")
        seed.releaseStage = "wham"

        seed.appVersion = "1.2.3"
        seed.appType = "custom"

        val endpoints = EndpointConfiguration("http://example.com:1234", "http://example.com:1235")
        seed.endpoints = endpoints
        seed.launchDurationMillis = 7000
        seed.maxBreadcrumbs = 37
        seed.maxPersistedEvents = 55
        seed.maxPersistedSessions = 103
        seed.persistUser = true
        seed.enabledBreadcrumbTypes = emptySet()
        seed.sendLaunchCrashesSynchronously = false

        // verify overrides are copied across
        with(convertToImmutableConfig(seed, ValueProvider("f7ab"))) {
            assertEquals("5d1ec5bd39a74caa1267142706a7fb21", apiKey)

            // detection
            assertFalse(autoTrackSessions)
            assertNotSame(seed.enabledErrorTypes, enabledErrorTypes)
            assertFalse(enabledErrorTypes.unhandledExceptions)
            assertFalse(enabledErrorTypes.anrs)
            assertFalse(enabledErrorTypes.ndkCrashes)
            assertEquals(ThreadSendPolicy.UNHANDLED_ONLY, sendThreads)

            // release stages
            assertEquals(discardClasses, discardClasses)
            assertEquals(setOf("bar"), enabledReleaseStages)
            assertEquals(setOf("com.example"), projectPackages)
            assertEquals("wham", releaseStage)

            // identifiers
            assertEquals("1.2.3", seed.appVersion)
            assertEquals("f7ab", buildUuid?.getOrNull())
            assertEquals("custom", seed.appType)

            // network config
            val endpoints1 = seed.endpoints
            assertEquals(endpoints1.notify, endpoints.notify)
            assertEquals(endpoints1.sessions, endpoints.sessions)

            // behaviour
            assertEquals(7000, seed.launchDurationMillis)
            assertFalse(sendLaunchCrashesSynchronously)
            assertEquals(NoopLogger, seed.logger)
            assertEquals(37, seed.maxBreadcrumbs)
            assertEquals(55, seed.maxPersistedEvents)
            assertEquals(103, seed.maxPersistedSessions)
            assertTrue(seed.persistUser)
            assertTrue(seed.enabledBreadcrumbTypes!!.isEmpty())
            assertNotNull(persistenceDirectory)
        }
    }

    @Test
    fun configSanitisationDevelopment() {
        `when`(context.packageName).thenReturn("com.example.foo")
        val appInfo = ApplicationInfo()
        appInfo.flags = ApplicationInfo.FLAG_DEBUGGABLE
        `when`(
            packageManager.getApplicationInfo(
                "com.example.foo",
                PackageManager.GET_META_DATA
            )
        ).thenReturn(appInfo)
        `when`(context.packageManager).thenReturn(packageManager)
        val cacheDir = Files.createTempDirectory("foo").toFile()
        `when`(context.cacheDir).thenReturn(cacheDir)
        val packageInfo = PackageInfo()
        @Suppress("DEPRECATION")
        packageInfo.versionCode = 55
        `when`(packageManager.getPackageInfo("com.example.foo", 0)).thenReturn(packageInfo)

        val seed = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        seed.logger = NoopLogger
        val config = sanitiseConfiguration(context, seed, connectivity, backgroundTaskService)
        assertEquals(NoopLogger, config.logger)
        assertEquals(setOf("com.example.foo"), config.projectPackages)
        assertEquals("development", config.releaseStage)
        assertEquals(55, config.versionCode)
        assertNotNull(config.delivery)
        assertEquals(cacheDir, config.persistenceDirectory.value)
    }

    @Test
    fun configSanitisationProduction() {
        `when`(context.packageName).thenReturn("com.example.foo")
        `when`(context.packageManager).thenReturn(packageManager)
        val cacheDir = Files.createTempDirectory("foo").toFile()
        `when`(context.cacheDir).thenReturn(cacheDir)
        val packageInfo = PackageInfo()
        @Suppress("DEPRECATION")
        packageInfo.versionCode = 55
        `when`(packageManager.getPackageInfo("com.example.foo", 0)).thenReturn(packageInfo)

        val seed = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        seed.logger = NoopLogger
        val config = sanitiseConfiguration(context, seed, connectivity, backgroundTaskService)
        assertEquals(NoopLogger, config.logger)
        assertEquals(setOf("com.example.foo"), config.projectPackages)
        assertEquals("production", config.releaseStage)
        assertEquals(55, config.versionCode)
        assertNotNull(config.delivery)
        assertEquals(cacheDir, config.persistenceDirectory.value)
    }

    @Test
    fun sanitizeConfigBuildUuidString() {
        `when`(context.packageName).thenReturn("com.example.foo")
        `when`(context.packageManager).thenReturn(packageManager)

        // setup build uuid
        val bundle = mock(Bundle::class.java)
        `when`(bundle.containsKey("com.bugsnag.android.BUILD_UUID")).thenReturn(true)
        `when`(bundle.getString("com.bugsnag.android.BUILD_UUID")).thenReturn("6533e9f7-0e98-40fe-84b4-0e4ed6df6866")
        val appInfo = ApplicationInfo().apply { metaData = bundle }
        `when`(packageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(appInfo)

        // validate build uuid
        val seed = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        val config = sanitiseConfiguration(context, seed, connectivity, backgroundTaskService)
        assertEquals("6533e9f7-0e98-40fe-84b4-0e4ed6df6866", config.buildUuid?.getOrNull())
    }

    @Test
    fun sanitizeConfigEmptyBuildUuid() {
        `when`(context.packageName).thenReturn("com.example.foo")
        `when`(context.packageManager).thenReturn(packageManager)

        // setup build uuid
        val bundle = mock(Bundle::class.java)
        `when`(bundle.containsKey("com.bugsnag.android.BUILD_UUID")).thenReturn(true)
        `when`(bundle.getString("com.bugsnag.android.BUILD_UUID")).thenReturn("")
        val appInfo = ApplicationInfo().apply { metaData = bundle }
        `when`(packageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(appInfo)

        // validate build uuid
        val seed = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        val config = sanitiseConfiguration(context, seed, connectivity, backgroundTaskService)
        assertNull(config.buildUuid?.getOrNull())
    }

    @Test
    fun sanitizeConfigBuildUuidInt() {
        `when`(context.packageName).thenReturn("com.example.foo")
        `when`(context.packageManager).thenReturn(packageManager)

        // setup build uuid
        val bundle = mock(Bundle::class.java)
        `when`(bundle.containsKey("com.bugsnag.android.BUILD_UUID")).thenReturn(true)
        `when`(bundle.getString("com.bugsnag.android.BUILD_UUID")).thenReturn(null)
        `when`(bundle.getInt("com.bugsnag.android.BUILD_UUID")).thenReturn(590265330)
        val appInfo = ApplicationInfo().apply { metaData = bundle }
        `when`(packageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(appInfo)

        // validate build uuid
        val seed = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        val config = sanitiseConfiguration(context, seed, connectivity, backgroundTaskService)
        assertEquals("590265330", config.buildUuid?.getOrNull())
    }

    @Test
    fun sanitizeConfigNoBuildUuid() {
        `when`(context.packageName).thenReturn("com.example.foo")
        `when`(context.packageManager).thenReturn(packageManager)

        // setup build uuid
        val bundle = mock(Bundle::class.java)
        `when`(bundle.containsKey("com.bugsnag.android.BUILD_UUID")).thenReturn(false)
        val appInfo = ApplicationInfo().apply { metaData = bundle }
        `when`(packageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(appInfo)

        // validate build uuid
        val seed = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        val config = sanitiseConfiguration(context, seed, connectivity, backgroundTaskService)
        assertNull(config.buildUuid?.getOrNull())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testEmptyApiKey() {
        val seed = Configuration("")
        `when`(isInvalidApiKey(seed.apiKey)).thenThrow(IllegalArgumentException())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSettingEmptyApiKey() {
        val seed = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        seed.apiKey = ""
        `when`(isInvalidApiKey(seed.apiKey)).thenThrow(IllegalArgumentException())
        val config = sanitiseConfiguration(context, seed, connectivity, backgroundTaskService)

        assertEquals("", config.apiKey)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSettingNullApiKey() {
        val seed = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        seed.apiKey = ""
        `when`(isInvalidApiKey(seed.apiKey)).thenThrow(IllegalArgumentException())
        val config = sanitiseConfiguration(context, seed, connectivity, backgroundTaskService)

        assertEquals(null, config.apiKey)
    }

    @Test
    fun testSettingWrongSizeApiKey() {
        val config = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        config.apiKey = "abfe05f"
        assertTrue(isInvalidApiKey(config.apiKey))
        assertEquals("abfe05f", config.apiKey)
    }
}
