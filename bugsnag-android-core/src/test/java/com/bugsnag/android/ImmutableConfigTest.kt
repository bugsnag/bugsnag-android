package com.bugsnag.android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
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
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.nio.file.Files

@RunWith(MockitoJUnitRunner::class)
internal class ImmutableConfigTest {

    private val seed = generateConfiguration()

    @Mock
    lateinit var delivery: Delivery

    @Mock
    lateinit var connectivity: Connectivity

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var packageManager: PackageManager

    @Before
    fun setUp() {
        // these options are required, but are set in the Client constructor if no value is set
        // on the config object
        seed.delivery = delivery
        seed.logger = NoopLogger
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
            @Suppress("DEPRECATION") // tests deprecated option is set via launchDurationMillis
            assertEquals(seed.launchCrashThresholdMs, launchDurationMillis)
            assertEquals(seed.launchDurationMillis, launchDurationMillis)
            assertTrue(sendLaunchCrashesSynchronously)
            assertEquals(NoopLogger, seed.logger)
            assertEquals(seed.maxBreadcrumbs, maxBreadcrumbs)
            assertEquals(seed.maxPersistedEvents, maxPersistedEvents)
            assertEquals(seed.maxPersistedSessions, maxPersistedSessions)
            assertEquals(seed.persistUser, persistUser)
            assertEquals(seed.enabledBreadcrumbTypes, BreadcrumbType.values().toSet())
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

        seed.discardClasses = setOf("foo")
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
        with(convertToImmutableConfig(seed, "f7ab")) {
            assertEquals("5d1ec5bd39a74caa1267142706a7fb21", apiKey)

            // detection
            assertFalse(autoTrackSessions)
            assertNotSame(seed.enabledErrorTypes, enabledErrorTypes)
            assertFalse(enabledErrorTypes.unhandledExceptions)
            assertFalse(enabledErrorTypes.anrs)
            assertFalse(enabledErrorTypes.ndkCrashes)
            assertEquals(ThreadSendPolicy.UNHANDLED_ONLY, sendThreads)

            // release stages
            assertEquals(setOf("foo"), discardClasses)
            assertEquals(setOf("bar"), enabledReleaseStages)
            assertEquals(setOf("com.example"), projectPackages)
            assertEquals("wham", releaseStage)

            // identifiers
            assertEquals("1.2.3", seed.appVersion)
            assertEquals("f7ab", buildUuid)
            assertEquals("custom", seed.appType)

            // network config
            val endpoints1 = seed.endpoints
            assertEquals(endpoints1.notify, endpoints.notify)
            assertEquals(endpoints1.sessions, endpoints.sessions)

            // behaviour
            assertEquals(7000, seed.launchDurationMillis)
            @Suppress("DEPRECATION") // should be same as launchDurationMillis
            assertEquals(7000, seed.launchCrashThresholdMs)
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
        `when`(packageManager.getApplicationInfo("com.example.foo", PackageManager.GET_META_DATA)).thenReturn(appInfo)
        `when`(context.packageManager).thenReturn(packageManager)
        val cacheDir = Files.createTempDirectory("foo").toFile()
        `when`(context.cacheDir).thenReturn(cacheDir)
        val packageInfo = PackageInfo()
        @Suppress("DEPRECATION")
        packageInfo.versionCode = 55
        `when`(packageManager.getPackageInfo("com.example.foo", 0)).thenReturn(packageInfo)

        val seed = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        seed.logger = NoopLogger
        val config = sanitiseConfiguration(context, seed, connectivity)
        assertEquals(NoopLogger, config.logger)
        assertEquals(setOf("com.example.foo"), config.projectPackages)
        assertEquals("development", config.releaseStage)
        assertEquals(55, config.versionCode)
        assertNotNull(config.delivery)
        assertEquals(cacheDir, config.persistenceDirectory)
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
        val config = sanitiseConfiguration(context, seed, connectivity)
        assertEquals(NoopLogger, config.logger)
        assertEquals(setOf("com.example.foo"), config.projectPackages)
        assertEquals("production", config.releaseStage)
        assertEquals(55, config.versionCode)
        assertNotNull(config.delivery)
        assertEquals(cacheDir, config.persistenceDirectory)
    }
}
