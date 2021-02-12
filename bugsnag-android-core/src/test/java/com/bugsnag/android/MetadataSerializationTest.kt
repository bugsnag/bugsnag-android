package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.util.HashMap
import java.util.LinkedList

@RunWith(Parameterized::class)
internal class MetadataSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases() = generateSerializationTestCases(
            "meta_data",
            Metadata(),
            basic(),
            nestedMap(),
            nestedCollection(),
            clearedTab()
        )

        private fun basic(): Metadata {
            val metadata = Metadata()
            metadata.addMetadata("example", "string", "value")
            metadata.addMetadata("example", "integer", 123)
            metadata.addMetadata("example", "double", 123.45)
            metadata.addMetadata("example", "boolean", true)
            metadata.addMetadata("example", "null", null)
            metadata.addMetadata("example", "array", arrayOf("a", "b"))
            val strings = listOf("Hello", "World")
            metadata.addMetadata("example", "collection", strings)

            val map = HashMap<String, String>()
            map["key"] = "value"
            metadata.addMetadata("example", "map", map)
            return metadata
        }

        private fun nestedMap(): Metadata {
            val childMap = HashMap<String, String>()
            childMap["key"] = "value"

            val map = HashMap<String, Any>()
            map["key"] = childMap

            val metadata = Metadata()
            metadata.addMetadata("example", "map", map)
            return metadata
        }

        private fun nestedCollection(): Metadata {
            val childList = LinkedList<String>()
            childList.add("james")
            childList.add("test")

            val list = LinkedList<Collection<String>>()
            list.add(childList)

            val metadata = Metadata()
            metadata.addMetadata("example", "list", list)
            return metadata
        }

        private fun clearedTab(): Metadata {
            val metadata = Metadata()
            metadata.addMetadata("example", "string", "value")
            metadata.clearMetadata("example", "string")
            return metadata
        }
    }

    @Parameter
    lateinit var testCase: Pair<Metadata, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
