package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class MetaDataDeserializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases() = generateDeserializationTestCases("meta_data", MetaData())
    }

    @Parameter
    lateinit var testCase: Pair<MetaData, String>

    @Test
    fun testJsonDeserialisation() {
        val reader = JsonParser().parse(testCase.second)
        val metaData = ErrorReader.readMetaData(reader)

        val expected = testCase.first
        assertEquals(expected.store, metaData.store)
    }
}
