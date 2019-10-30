package com.bugsnag.android.ndk

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.*

@RunWith(Parameterized::class)
internal class CustomMetadataSerializationTest {

    companion object {
        init {
            System.loadLibrary("bugsnag-ndk")
            System.loadLibrary("bugsnag-ndk-test")
        }

        @JvmStatic
        @Parameters
        fun testCases() = 0..0
    }

    external fun run(testCase: Int, expectedJson: String): Int

    @Parameter
    lateinit var testCase: Number

    @Test
    fun testPassesNativeSuite() {
        val expectedJson = loadJson("custom_meta_data_serialization_$testCase.json")
        verifyNativeRun(run(testCase.toInt(), expectedJson))
    }
}
