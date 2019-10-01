package com.bugsnag.android

import android.content.Context
import android.content.pm.PackageManager
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import org.mockito.Mockito

@RunWith(Parameterized::class)
internal class AppDataSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Map<String, Any>, String>> {
            val context = Mockito.mock(Context::class.java)
            val pm = Mockito.mock(PackageManager::class.java)

            val config = Configuration("api-key")
            val appData = AppData(context, pm, config, null)

            return generateTestCases(
                "app_data", appData.appDataSummary
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<Map<String, Any>, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
