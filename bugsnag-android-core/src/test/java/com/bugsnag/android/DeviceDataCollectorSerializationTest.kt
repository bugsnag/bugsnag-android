package com.bugsnag.android

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import com.bugsnag.android.BugsnagTestUtils.generateDeviceBuildInfo
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.dag.ValueProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

@RunWith(Parameterized::class)
internal class DeviceDataCollectorSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Device, String>> {
            val context = mock(Context::class.java)
            val res = mock(Resources::class.java)
            val conf = mock(Configuration::class.java)
            val connectivity = mock(Connectivity::class.java)

            val prefs = mock(SharedPreferences::class.java)
            val editor = mock(SharedPreferences.Editor::class.java)
            `when`(prefs.edit()).thenReturn(editor)
            `when`(editor.putString(anyString(), anyString()))
                .thenReturn(editor)

            val buildInfo = generateDeviceBuildInfo()

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
            val deviceData = DeviceDataCollector(
                connectivity,
                context,
                res,
                ValueProvider(DeviceIdStore.DeviceIds("123", "456")),
                buildInfo,
                File(""),
                ValueProvider(false),
                BackgroundTaskService(),
                NoopLogger
            )

            return generateSerializationTestCases(
                "device_data",
                deviceData.generateDevice(),
                deviceData.generateDeviceWithState(0)
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<Device, String>

    private val eventMapper = BugsnagEventMapper(NoopLogger)

    @Test
    fun testJsonSerialisation() {
        // sanitise device-specific fields before serializing
        val device = testCase.first
        device.totalMemory = 502934020442
        device.locale = "en_GB"

        if (device is DeviceWithState) {
            device.freeMemory = 2234092234234
            device.freeDisk = 120935100007
        }

        verifyJsonMatches(device, testCase.second)
    }

    @Test
    fun testJsonDeserialization() {
        verifyJsonParser(testCase.first, testCase.second) {
            eventMapper.convertDeviceWithState(it)
        }
    }
}
