package com.bugsnag.android

import android.os.Bundle
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestConfigLoaderTest {

    private val configLoader = ManifestConfigLoader()

    @Test(expected = IllegalArgumentException::class)
    fun testMissingApiKey() {
        configLoader.load(Bundle())
    }

    @Test
    fun testManifestLoadsDefaults() {
        val data = Bundle()
        data.putString("com.bugsnag.android.API_KEY", "abc123")
        val config = configLoader.load(data)

        with(config) {
            assertEquals(apiKey, "abc123")
            assertNull(buildUuid)

            // detection
            assertTrue(autoNotify)
            assertFalse(detectAnrs)
            assertFalse(detectNdkCrashes)
            assertTrue(autoCaptureSessions)
            assertTrue(autoCaptureBreadcrumbs)
            assertTrue(sendThreads)
            assertFalse(persistUserBetweenSessions)

            // endpoints
            assertEquals(endpoints.notify, "https://notify.bugsnag.com")
            assertEquals(endpoints.sessions, "https://sessions.bugsnag.com")

            // app/project packages
            assertNull(appVersion)
            assertEquals(0, versionCode)
            assertNull(releaseStage)
            assertEquals(emptySet<String>(), notifyReleaseStages)
            assertEquals(emptySet<String>(), ignoreClasses)
            assertEquals(emptySet<String>(), projectPackages)
            assertEquals(setOf("password"), filters)

            // misc
            assertEquals(maxBreadcrumbs, 32)
            assertEquals(launchCrashThresholdMs, 5000)
        }
    }

    @Test
    fun testManifestOverridesDefaults() {
        val data = Bundle().apply {
            putString("com.bugsnag.android.API_KEY", "abc123")
            putString("com.bugsnag.android.BUILD_UUID", "fgh123456")

            // detection
            putBoolean("com.bugsnag.android.AUTO_NOTIFY", false)
            putBoolean("com.bugsnag.android.DETECT_ANRS", true)
            putBoolean("com.bugsnag.android.DETECT_NDK_CRASHES", true)
            putBoolean("com.bugsnag.android.AUTO_CAPTURE_SESSIONS", false)
            putBoolean("com.bugsnag.android.AUTO_CAPTURE_BREADCRUMBS", false)
            putBoolean("com.bugsnag.android.SEND_THREADS", false)
            putBoolean("com.bugsnag.android.PERSIST_USER_BETWEEN_SESSIONS", true)

            // endpoints
            putString("com.bugsnag.android.ENDPOINT", "http://localhost:1234")
            putString("com.bugsnag.android.SESSIONS_ENDPOINT", "http://localhost:2345")

            // app/project packages
            putString("com.bugsnag.android.APP_VERSION", "5.23.7")
            putInt("com.bugsnag.android.VERSION_CODE", 55)
            putString("com.bugsnag.android.RELEASE_STAGE", "beta")
            putString("com.bugsnag.android.NOTIFY_RELEASE_STAGES", "beta,production,staging")
            putString("com.bugsnag.android.IGNORE_CLASSES", "com.bugsnag.FooKt,org.example.String")
            putString("com.bugsnag.android.PROJECT_PACKAGES", "com.bugsnag,com.example")
            putString("com.bugsnag.android.FILTERS", "password,auth,foo")

            // misc
            putInt("com.bugsnag.android.MAX_BREADCRUMBS", 50)
            putInt("com.bugsnag.android.LAUNCH_CRASH_THRESHOLD_MS", 7000)
        }

        val config = configLoader.load(data)

        with(config) {
            assertEquals("abc123", apiKey)
            assertEquals("fgh123456", buildUuid)

            // detection
            assertFalse(autoNotify)
            assertTrue(detectAnrs)
            assertTrue(detectNdkCrashes)
            assertFalse(autoCaptureSessions)
            assertFalse(autoCaptureBreadcrumbs)
            assertFalse(sendThreads)
            assertTrue(persistUserBetweenSessions)

            // endpoints
            assertEquals(endpoints.notify, "http://localhost:1234")
            assertEquals(endpoints.sessions, "http://localhost:2345")

            // app/project packages
            assertEquals("5.23.7", appVersion)
            assertEquals(55, versionCode)
            assertEquals("beta", releaseStage)
            assertEquals(setOf("beta", "production", "staging"), notifyReleaseStages)
            assertEquals(setOf("com.bugsnag.FooKt", "org.example.String"), ignoreClasses)
            assertEquals(setOf("com.bugsnag", "com.example"), projectPackages)
            assertEquals(setOf("password", "auth", "foo"), filters)

            // misc
            assertEquals(maxBreadcrumbs, 50)
            assertEquals(launchCrashThresholdMs, 7000)
        }
    }

    @Test
    fun testManifestAliases() {
        val data = Bundle().apply {
            putString("com.bugsnag.android.API_KEY", "abc123")
            putBoolean("com.bugsnag.android.ENABLE_EXCEPTION_HANDLER", false)
        }

        val config = configLoader.load(data)

        with(config) {
            assertEquals("abc123", apiKey)
            assertFalse(autoNotify)
        }
    }
}
