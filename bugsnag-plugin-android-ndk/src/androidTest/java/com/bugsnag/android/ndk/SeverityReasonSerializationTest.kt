package com.bugsnag.android.ndk

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class SeverityReasonSerializationTest {

    companion object {
        init {
            System.loadLibrary("bugsnag-ndk")
            System.loadLibrary("bugsnag-ndk-test")
        }

        @JvmStatic
        @Parameterized.Parameters
        fun testCases() = 0..0
    }

    external fun run(testCase: Int, expectedJson: String): Int

    @Parameterized.Parameter
    lateinit var testCase: Number

    @Test
    fun testPassesNativeSuite() {
        val expectedJson = loadJson("severity_reason_serialization_$testCase.json")
        verifyNativeRun(run(testCase.toInt(), expectedJson))
    }
}
