package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class MetadataSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases() = generateSerializationTestCases(
            "meta_data",
            Metadata()
        )
    }

    @Parameter
    lateinit var testCase: Pair<Metadata, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
