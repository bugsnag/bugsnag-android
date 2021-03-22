package com.bugsnag.android

import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestConfigLoaderTest {

    private val configLoader = ManifestConfigLoader()

    @Test(expected = IllegalArgumentException::class)
    fun testMissingApiKey() {
        configLoader.load(Bundle(), null)
    }

    @Test
    fun testManifestLoadsDefaults() {
        val data = Bundle()
        data.putString("com.bugsnag.android.API_KEY", "5d1ec5bd39a74caa1267142706a7fb21")
        val config = configLoader.load(data, null)

        with(config) {
            assertEquals(apiKey, "5d1ec5bd39a74caa1267142706a7fb21")

            // detection
            assertTrue(autoDetectErrors)
            assertTrue(autoTrackSessions)
            assertEquals(ThreadSendPolicy.ALWAYS, sendThreads)
            assertFalse(persistUser)

            // endpoints
            assertEquals(endpoints.notify, "https://notify.bugsnag.com")
            assertEquals(endpoints.sessions, "https://sessions.bugsnag.com")

            // app/project packages
            assertNull(appVersion)
            assertEquals(0, versionCode)
            assertNull(releaseStage)
            assertNull(enabledReleaseStages)
            assertEquals(emptySet<String>(), discardClasses)
            assertEquals(emptySet<String>(), projectPackages)
            assertEquals(setOf("password"), redactedKeys)

            // misc
            assertEquals(maxBreadcrumbs, 25)
            assertEquals(maxPersistedEvents, 32)
            assertEquals(maxPersistedSessions, 128)
            assertTrue(sendLaunchCrashesSynchronously)
            @Suppress("DEPRECATION")
            assertEquals(launchCrashThresholdMs, 5000)
            assertEquals(launchDurationMillis, 5000)
            assertEquals("android", appType)
        }
    }

    @Test
    fun testManifestOverridesDefaults() {
        val data = Bundle().apply {
            putString("com.bugsnag.android.API_KEY", "5d1ec5bd39a74caa1267142706a7fb21")
            putString("com.bugsnag.android.BUILD_UUID", "fgh123456")

            // detection
            putBoolean("com.bugsnag.android.AUTO_TRACK_SESSIONS", false)
            putBoolean("com.bugsnag.android.AUTO_DETECT_ERRORS", false)
            putBoolean("com.bugsnag.android.AUTO_CAPTURE_BREADCRUMBS", false)
            putBoolean("com.bugsnag.android.PERSIST_USER", true)
            putString("com.bugsnag.android.SEND_THREADS", "UNHANDLED_ONLY")

            // endpoints
            putString("com.bugsnag.android.ENDPOINT_NOTIFY", "http://localhost:1234")
            putString("com.bugsnag.android.ENDPOINT_SESSIONS", "http://localhost:2345")

            // app/project packages
            putString("com.bugsnag.android.APP_VERSION", "5.23.7")
            putInt("com.bugsnag.android.VERSION_CODE", 55)
            putString("com.bugsnag.android.RELEASE_STAGE", "beta")
            putString("com.bugsnag.android.ENABLED_RELEASE_STAGES", "beta,production,staging")
            putString("com.bugsnag.android.DISCARD_CLASSES", "com.bugsnag.FooKt,org.example.String")
            putString("com.bugsnag.android.PROJECT_PACKAGES", "com.bugsnag,com.example")
            putString("com.bugsnag.android.REDACTED_KEYS", "password,auth,foo")

            // misc
            putInt("com.bugsnag.android.MAX_BREADCRUMBS", 50)
            putInt("com.bugsnag.android.MAX_PERSISTED_EVENTS", 52)
            putInt("com.bugsnag.android.MAX_PERSISTED_SESSIONS", 64)
            putInt("com.bugsnag.android.LAUNCH_DURATION_MILLIS", 7000)
            putBoolean("com.bugsnag.android.SEND_LAUNCH_CRASHES_SYNCHRONOUSLY", false)
            putString("com.bugsnag.android.APP_TYPE", "react-native")
            putString("com.bugsnag.android.CODE_BUNDLE_ID", "123")
        }

        val config = configLoader.load(data, null)

        with(config) {
            assertEquals("5d1ec5bd39a74caa1267142706a7fb21", apiKey)

            // detection
            assertFalse(autoDetectErrors)
            assertFalse(autoTrackSessions)
            assertEquals(ThreadSendPolicy.UNHANDLED_ONLY, sendThreads)
            assertTrue(persistUser)

            // endpoints
            assertEquals(endpoints.notify, "http://localhost:1234")
            assertEquals(endpoints.sessions, "http://localhost:2345")

            // app/project packages
            assertEquals("5.23.7", appVersion)
            assertEquals(55, versionCode)
            assertEquals("beta", releaseStage)
            assertEquals(setOf("beta", "production", "staging"), enabledReleaseStages)
            assertEquals(setOf("com.bugsnag.FooKt", "org.example.String"), discardClasses)
            assertEquals(setOf("com.bugsnag", "com.example"), projectPackages)
            assertEquals(setOf("password", "auth", "foo"), redactedKeys)

            // misc
            assertEquals(maxBreadcrumbs, 50)
            assertEquals(maxPersistedEvents, 52)
            assertEquals(maxPersistedSessions, 64)
            @Suppress("DEPRECATION")
            assertEquals(launchCrashThresholdMs, 7000)
            assertEquals(launchDurationMillis, 7000)
            assertFalse(sendLaunchCrashesSynchronously)
            assertEquals("react-native", appType)
        }
    }

    @Test
    fun testManifestAliases() {
        val data = Bundle().apply {
            putString("com.bugsnag.android.API_KEY", "5d1ec5bd39a74caa1267142706a7fb21")
            putBoolean("com.bugsnag.android.ENABLE_EXCEPTION_HANDLER", false)
            putInt("com.bugsnag.android.LAUNCH_CRASH_THRESHOLD_MS", 8000)
        }

        val config = configLoader.load(data, null)

        with(config) {
            assertEquals("5d1ec5bd39a74caa1267142706a7fb21", apiKey)
            assertEquals(8000, launchDurationMillis)
        }
    }
}
