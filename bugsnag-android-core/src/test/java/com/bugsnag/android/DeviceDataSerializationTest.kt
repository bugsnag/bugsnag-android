package com.bugsnag.android

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import org.mockito.Mockito
import org.mockito.Mockito.*
import java.io.File

@RunWith(Parameterized::class)
internal class DeviceDataSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Map<String, Any>, String>> {
            val context = mock(Context::class.java)
            val res = mock(Resources::class.java)
            val conf = mock(Configuration::class.java)
            val connectivity = mock(Connectivity::class.java)

            val prefs = mock(SharedPreferences::class.java)
            val editor = mock(SharedPreferences.Editor::class.java)
            `when`(prefs.edit()).thenReturn(editor)
            `when`(editor.putString(anyString(), anyString()))
                .thenReturn(editor)

            val buildInfo = DeviceBuildInfo(
                "samsung", "s7", "7.1", 24, "bulldog",
                "foo-google", "prod,build", "google", arrayOf("armeabi-v7a")
            )

            // regular fields
            `when`(res.configuration).thenReturn(conf)
            conf.orientation = 2

            // metadata fields
            val metrics = DisplayMetrics()
            metrics.density = 200f
            metrics.densityDpi = 120
            `when`(res.displayMetrics).thenReturn(metrics)
            `when`(connectivity.retrieveNetworkAccessState()).thenReturn("unknown")

            // construct devicedata object
            val deviceData = DeviceData(connectivity, context, res, "123", buildInfo, File(""), NoopLogger)

            // serializes the 3 different maps that DeviceData can generate:
            // 1. summary (used in session payloads)
            // 2. regular (used in event payloads)
            // 3. metadata (used in event payloads)
            val regularMap = deviceData.deviceData
            arrayOf("freeDisk", "freeMemory", "totalMemory", "locale", "time").forEach { regularMap.remove(it) }

            return generateSerializationTestCases(
                "device_data",
                deviceData.deviceDataSummary,
                regularMap,
                deviceData.deviceMetadata
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<Map<String, Any>, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
