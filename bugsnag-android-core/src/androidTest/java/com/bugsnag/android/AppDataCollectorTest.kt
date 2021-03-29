package com.bugsnag.android

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
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
    fun testNoBackgroundRestrictions() {
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
            NoopLogger
        )
        val app = collector.getAppDataMetadata()
        assertNull(app["backgroundWorkRestricted"])
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
            NoopLogger
        )
        val app = collector.getAppDataMetadata()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            assertTrue(app["backgroundWorkRestricted"] as Boolean)
        } else {
            assertNull(app["backgroundWorkRestricted"])
        }
    }
}
