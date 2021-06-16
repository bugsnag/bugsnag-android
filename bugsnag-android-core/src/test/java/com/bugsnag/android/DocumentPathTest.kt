package com.bugsnag.android

import org.junit.Assert
import org.junit.Test
import java.util.LinkedList
import kotlin.collections.HashMap

class DocumentPathTest {
    @Test
    fun testReplaceDocument() {
        val document = HashMap<String, Any>()
        val docpath = DocumentPath("")
        val value = mapOf<String, Any>("a" to 1)
        val expected = mapOf<String, Any>("a" to 1)

        val actual = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testMapPut() {
        val document = HashMap<String, Any>()
        val docpath = DocumentPath("a")
        val value = 1
        val expected = mapOf<String, Any>("a" to 1)

        val actual = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testMapModify() {
        val document = HashMap<String, Any>(mapOf("a" to 1))
        val docpath = DocumentPath("a")
        val value = 2
        val expected = mapOf<String, Any>("a" to 2)

        val actual = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testMapDelete() {
        val document = HashMap<String, Any>(mapOf("a" to 1))
        val docpath = DocumentPath("a")
        val value = null
        val expected = mapOf<String, Any>()

        val actual = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testMapLevel2Put() {
        val document = HashMap<String, Any>()
        val docpath = DocumentPath("a.b")
        val value = 1
        val expected = mapOf("a" to mapOf("b" to 1))

        val actual = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testMapLevel2PutEscapes() {
        val document = HashMap<String, Any>()
        val docpath = DocumentPath("a\\.x.b\\.y")
        val value = 1
        val expected = mapOf("a.x" to mapOf("b.y" to 1))

        val actual = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, actual)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEscape() {
        DocumentPath("a\\.x.b\\.y\\")
    }

    @Test
    fun testListAppendEmpty() {
        val document = HashMap<String, Any>()
        val docpath = DocumentPath("a.-1")
        val value = 1
        val expected = mapOf<String, Any>("a" to listOf<Any>(1))

        val actual = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testListDeleteLast() {
        val document = HashMap<String, Any>(
            mapOf(
                "a" to LinkedList<Any>(listOf<Any>(1, 2, 3))
            )
        )
        val docpath = DocumentPath("a.-1")
        val value = null
        val expected = mapOf<String, Any>("a" to listOf<Any>(1, 2))

        val actual = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testListAppendEmptyNull() {
        val document = HashMap<String, Any>()
        val docpath = DocumentPath("a.-1")
        val value = null
        val expected = mapOf<String, Any>("a" to listOf<Any>())

        val actual = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testListReplace() {
        val document = HashMap<String, Any>(mapOf<String, Any>("a" to LinkedList<Any>(listOf<Any>(1, 2, 3))))
        val docpath = DocumentPath("a.0")
        val value = 9
        val expected = mapOf<String, Any>("a" to listOf<Any>(9, 2, 3))

        val actual = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testListDelete() {
        val document = HashMap<String, Any>(mapOf<String, Any>("a" to LinkedList<Any>(listOf<Any>(1, 2, 3))))
        val docpath = DocumentPath("a.0")
        val value = null
        val expected = mapOf<String, Any>("a" to listOf<Any>(2, 3))

        val actual = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testListReplaceLast() {
        val document = HashMap<String, Any>(mapOf<String, Any>("a" to LinkedList<Any>(listOf<Any>(5, 4, 3, 2, 1))))
        val docpath = DocumentPath("a.-1")
        val value = 9
        val expected = mapOf<String, Any>("a" to listOf<Any>(5, 4, 3, 2, 9))

        val actual = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testListCreateAndAppend() {
        val document = HashMap<String, Any>()
        val docpath = DocumentPath("a.")
        val value = 1
        val expected = mapOf<String, Any>("a" to listOf<Any>(1))

        val actual = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testListCreateAndAppendEscape() {
        val document = HashMap<String, Any>()
        val docpath = DocumentPath("a\\..")
        val value = 1
        val expected = mapOf<String, Any>("a." to listOf<Any>(1))

        val actual = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testEscapeLastDot() {
        val document = HashMap<String, Any>()
        val docpath = DocumentPath("a\\.")
        val value = 1
        val expected = mapOf<String, Any>("a." to 1)

        val actual = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, actual)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testLastDotNull() {
        val document = HashMap<String, Any>()
        val docpath = DocumentPath("a.")
        val value = null

        docpath.modifyDocument(document, value)
    }
}
