package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class MetaDataSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases() = generateTestCases(
            "meta_data",
            MetaData()
        )
    }

    @Parameter
    lateinit var testCase: Pair<MetaData, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
