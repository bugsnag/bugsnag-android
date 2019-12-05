package com.bugsnag.android

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.bugsnag.android.BugsnagTestUtils.convert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import org.mockito.Mockito.*

@RunWith(Parameterized::class)
internal class AppDataSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Map<String, Any>, String>> {
            val context = mock(Context::class.java)
            val pm = mock(PackageManager::class.java)
            val am = mock(ActivityManager::class.java)
            val sessionTracker = mock(SessionTracker::class.java)
            val config = BugsnagTestUtils.generateConfiguration()

            // populate summary fields
            config.appType = "React Native"
            config.releaseStage = "test-stage"
            config.appVersion = "1.2.3"
            config.versionCode = 55
            config.codeBundleId = "foo-99"

            // populate regular fields
            `when`(context.packageName).thenReturn("com.example.foo")
            config.buildUuid = "123456"

            // populate metadata fields
            `when`(sessionTracker.contextActivity).thenReturn("MyActivity")
            `when`(pm.getApplicationInfo(any(), anyInt())).thenReturn(ApplicationInfo())
            `when`(pm.getApplicationLabel(any())).thenReturn("MyApp")

            // construct AppData object
            val appData = AppData(context, pm, convert(config), sessionTracker, am, NoopLogger)
            appData.setBinaryArch("x86")

            // serializes the 3 different maps that AppData can generate:
            // 1. summary (used in session payloads)
            // 2. regular (used in event payloads)
            // 3. metadata (used in event payloads)
            val metadata = appData.appDataMetadata
            metadata.remove("memoryUsage")

            return generateSerializationTestCases(
                "app_data",
                appData.appDataSummary,
                appData.appData,
                metadata
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<Map<String, Any>, String>

    @Test
    fun testJsonSerialisation() {
        verifyJsonMatches(testCase.first, testCase.second)
    }
}
