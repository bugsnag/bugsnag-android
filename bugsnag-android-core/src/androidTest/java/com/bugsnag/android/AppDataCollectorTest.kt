package com.bugsnag.android

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AppDataCollectorTest {

    lateinit var client: Client
    lateinit var context: Context

    @Mock
    lateinit var am: ActivityManager

    /**
     * Generates a configuration and clears sharedPrefs values to begin the test with a clean slate
     */
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        client = BugsnagTestUtils.generateClient()
    }

    /**
     * The flag is not set in the app metadata if the user has not enabled background restrictions
     */
    @Test
    fun testDefaultValues() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            `when`(am.isBackgroundRestricted).thenReturn(false)
        }

        val collector = AppDataCollector(
            context,
            context.packageManager,
            client.immutableConfig,
            client.sessionTracker,
            am,
            client.launchCrashTracker,
            client.memoryTrimState
        )
        val app = collector.getAppDataMetadata()
        assertNull(app["backgroundWorkRestricted"])
        assertTrue(app.containsKey("installerPackage"))
        assertEquals("com.bugsnag.android.core.test", app["processName"] as String)
    }

    @Test
    fun testActiveScreenValue() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            `when`(am.isBackgroundRestricted).thenReturn(false)
        }

        val collector = AppDataCollector(
            context,
            context.packageManager,
            client.immutableConfig,
            client.sessionTracker,
            am,
            client.launchCrashTracker,
            client.memoryTrimState
        )
        client.context = "Some Custom Context"
        client.sessionTracker.updateContext("MyActivity", true)
        val app = collector.getAppDataMetadata()
        assertEquals("MyActivity", app["activeScreen"] as String)
    }

    /**
     * The flag is set in the app metadata if the user has enabled background restrictions
     */
    @Test
    fun testBackgroundRestrictionsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            `when`(am.isBackgroundRestricted).thenReturn(true)
        }
        val collector = AppDataCollector(
            context,
            context.packageManager,
            client.immutableConfig,
            client.sessionTracker,
            am,
            client.launchCrashTracker,
            client.memoryTrimState
        )
        val app = collector.getAppDataMetadata()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            assertTrue(app["backgroundWorkRestricted"] as Boolean)
        } else {
            assertNull(app["backgroundWorkRestricted"])
        }
    }

    @Test
    fun testGetInstallerPackageName() = withBuildSdkInt(Build.VERSION_CODES.Q) {
        val packageManager = mock(PackageManager::class.java)
        `when`(packageManager.getApplicationLabel(any())).thenReturn("Test App name")

        val collector = AppDataCollector(
            context,
            packageManager,
            client.immutableConfig,
            client.sessionTracker,
            am,
            client.launchCrashTracker,
            client.memoryTrimState
        )

        @Suppress("DEPRECATION")
        `when`(packageManager.getInstallerPackageName(any())).thenReturn("Test Installer name")

        val result = collector.getInstallerPackageName()
        assertEquals("Test Installer name", result)
    }
}
