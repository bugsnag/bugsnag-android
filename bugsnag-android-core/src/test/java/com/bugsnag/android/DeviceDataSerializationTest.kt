package com.bugsnag.android

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import org.mockito.Mockito

@RunWith(Parameterized::class)
internal class DeviceDataSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Map<String, Any>, String>> {
            val context = Mockito.mock(Context::class.java)
            val res = Mockito.mock(Resources::class.java)
            val prefs = Mockito.mock(SharedPreferences::class.java)
            val editor = Mockito.mock(SharedPreferences.Editor::class.java)
            Mockito.`when`(prefs.edit()).thenReturn(editor)
            Mockito.`when`(editor.putString(Mockito.anyString(), Mockito.anyString())).thenReturn(editor)

            val buildInfo = DeviceBuildInfo(
                "samsung", "s7", "7.1", 24, "bulldog",
                "foo-google", "prod,build", arrayOf("armeabi-v7a")
            )
            val deviceData = DeviceData(null, context, res, "123", buildInfo, NoopLogger)
            return generateSerializationTestCases("device_data", deviceData.deviceDataSummary)
        }
    }

    @Parameter
    lateinit var testCase: Pair<Map<String, Any>, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
