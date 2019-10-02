package com.bugsnag.android

import android.content.Context
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import org.mockito.Mockito
import java.io.BufferedReader
import java.io.StringReader

@RunWith(Parameterized::class)
internal class AppDataDeserializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Map<String, Any?>, String>> {
            val context = Mockito.mock(Context::class.java)
            val pm = Mockito.mock(PackageManager::class.java)

            val config = Configuration("api-key")
            val appData = AppData(context, pm, config, null)

            return generateDeserializationTestCases(
                "app_data", appData.appDataSummary
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<Map<String, Any?>, String>

    @Test
    fun testJsonDeserialisation() {
        val reader = JsonParser().parse(testCase.second)
        val appData = ErrorReader.readAppData(reader)
        val expected = testCase.first.filter { it.value != null }
        assertEquals(expected, appData)
    }
}
