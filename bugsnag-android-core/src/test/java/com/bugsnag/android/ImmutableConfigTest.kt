package com.bugsnag.android

import android.content.Context
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class ImmutableConfigTest {

    private val seed = generateConfiguration()

    @Mock
    lateinit var delivery: Delivery

    @Mock
    lateinit var connectivity: Connectivity

    @Mock
    lateinit var context: Context

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
            assertTrue(autoDetectErrors)
            assertFalse(autoDetectAnrs)
            assertFalse(autoDetectNdkCrashes)
            assertEquals(Thread.ThreadSendPolicy.ALWAYS, sendThreads)

            // release stages
            assertTrue(ignoreClasses.isEmpty())
            assertTrue(enabledReleaseStages.isEmpty())
            assertTrue(projectPackages.isEmpty())
            assertEquals(seed.releaseStage, releaseStage)

            // identifiers
            assertEquals(seed.appVersion, appVersion)
            assertEquals(seed.buildUuid, buildUuid)
            assertEquals(seed.codeBundleId, codeBundleId)
            assertEquals(seed.appType, appType)

            // network config
            assertEquals(seed.delivery, delivery)
            assertEquals(seed.endpoints, endpoints)

            // behaviour
            assertEquals(seed.launchCrashThresholdMs, launchCrashThresholdMs)
            assertEquals(NoopLogger, seed.logger)
            assertEquals(seed.maxBreadcrumbs, maxBreadcrumbs)
            assertEquals(seed.persistUser, persistUser)
            assertEquals(seed.enabledBreadcrumbTypes, BreadcrumbType.values().toSet())
        }
    }

    @Test
    fun convertWithOverrides() {
        seed.autoTrackSessions = false
        seed.autoDetectErrors = false
        seed.autoDetectAnrs = true
        seed.autoDetectNdkCrashes = true
        seed.sendThreads = Thread.ThreadSendPolicy.UNHANDLED_ONLY

        seed.ignoreClasses = setOf("foo")
        seed.enabledReleaseStages = setOf("bar")
        seed.projectPackages = setOf("com.example")
        seed.releaseStage = "wham"

        seed.appVersion = "1.2.3"
        seed.buildUuid = "f7ab"
        seed.codeBundleId = "codebundle123"
        seed.appType = "custom"

        val endpoints = EndpointConfiguration("http://example.com:1234", "http://example.com:1235")
        seed.endpoints = endpoints
        seed.launchCrashThresholdMs = 7000
        seed.maxBreadcrumbs = 37
        seed.persistUser = true
        seed.enabledBreadcrumbTypes = emptySet()

        // verify overrides are copied across
        with(convertToImmutableConfig(seed)) {
            assertEquals("5d1ec5bd39a74caa1267142706a7fb21", apiKey)

            // detection
            assertFalse(autoTrackSessions)
            assertFalse(autoDetectErrors)
            assertTrue(autoDetectAnrs)
            assertTrue(autoDetectNdkCrashes)
            assertEquals(Thread.ThreadSendPolicy.UNHANDLED_ONLY, sendThreads)

            // release stages
            assertEquals(setOf("foo"), ignoreClasses)
            assertEquals(setOf("bar"), enabledReleaseStages)
            assertEquals(setOf("com.example"), projectPackages)
            assertEquals("wham", releaseStage)

            // identifiers
            assertEquals("1.2.3", seed.appVersion)
            assertEquals("f7ab", seed.buildUuid)
            assertEquals("codebundle123", seed.codeBundleId)
            assertEquals("custom", seed.appType)

            // network config
            assertEquals(seed.endpoints.notify, endpoints.notify)
            assertEquals(seed.endpoints.sessions, endpoints.sessions)

            // behaviour
            assertEquals(7000, seed.launchCrashThresholdMs)
            assertEquals(NoopLogger, seed.logger)
            assertEquals(37, seed.maxBreadcrumbs)
            assertTrue(seed.persistUser)
            assertTrue(seed.enabledBreadcrumbTypes.isEmpty())
        }
    }

    @Test
    fun verifyErrorApiHeaders() {
        val config = convertToImmutableConfig(seed)
        val headers = config.getErrorApiDeliveryParams().headers
        assertEquals(config.apiKey, headers["Bugsnag-Api-Key"])
        assertNotNull(headers["Bugsnag-Sent-At"])
        assertNotNull(headers["Bugsnag-Payload-Version"])
    }

    @Test
    fun verifySessionApiHeaders() {
        val config = convertToImmutableConfig(seed)
        val headers = config.getSessionApiDeliveryParams().headers
        assertEquals(config.apiKey, headers["Bugsnag-Api-Key"])
        assertNotNull(headers["Bugsnag-Sent-At"])
        assertNotNull(headers["Bugsnag-Payload-Version"])
    }

    @Test
    fun configSanitisation() {
        Mockito.`when`(context.packageName).thenReturn("com.example.foo")
        val seed = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        seed.logger = NoopLogger
        val config = sanitiseConfiguration(context, seed, connectivity)
        assertEquals(setOf("com.example.foo"), config.projectPackages)

        assertNotNull(config.delivery)
    }
}
