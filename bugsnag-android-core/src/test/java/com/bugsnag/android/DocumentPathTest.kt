package com.bugsnag.android

import com.bugsnag.android.internal.DocumentPath
import org.junit.Assert
import org.junit.Test
import java.util.LinkedList

class DocumentPathTest {
    @Test
    fun testReplaceDocument() {
        val document = mutableMapOf<String, Any>()
        val docpath = DocumentPath("")
        val value = mapOf<String, Any>("a" to 1)
        val expected = mapOf<String, Any>("a" to 1)

        val observed = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testMapPut() {
        val document = mutableMapOf<String, Any>()
        val docpath = DocumentPath("a")
        val value = 1
        val expected = mapOf<String, Any>("a" to 1)

        val observed = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testMapModify() {
        val document = mutableMapOf<String, Any>("a" to 1)
        val docpath = DocumentPath("a")
        val value = 2
        val expected = mapOf<String, Any>("a" to 2)

        val observed = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testMapDelete() {
        val document = mutableMapOf<String, Any>("a" to 1)
        val docpath = DocumentPath("a")
        val value = null
        val expected = mapOf<String, Any>()

        val observed = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testMapLevel2Put() {
        val document = mutableMapOf<String, Any>()
        val docpath = DocumentPath("a.b")
        val value = 1
        val expected = mapOf("a" to mapOf("b" to 1))

        val observed = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testMapLevel2PutEscapes() {
        val document = mutableMapOf<String, Any>()
        val docpath = DocumentPath("a\\.x.b\\.y")
        val value = 1
        val expected = mapOf("a.x" to mapOf("b.y" to 1))

        val observed = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testMapLevel2PutEscapedBackslash() {
        val document = mutableMapOf<String, Any>()
        val docpath = DocumentPath("a\\.x.b\\\\y")
        val value = 1
        val expected = mapOf("a.x" to mapOf("b\\y" to 1))

        val obseved = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, obseved)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBadEscape() {
        DocumentPath("a\\.x.b\\.y\\")
    }

    @Test
    fun testListAppendEmpty() {
        val document = mutableMapOf<String, Any>()
        val docpath = DocumentPath("a.-1")
        val value = 1
        val expected = mapOf<String, Any>("a" to listOf<Any>(1))

        val observed = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testListDeleteLast() {
        val document = mutableMapOf<String, Any>("a" to LinkedList<Any>(listOf<Any>(1, 2, 3)))
        val docpath = DocumentPath("a.-1")
        val value = null
        val expected = mapOf<String, Any>("a" to listOf<Any>(1, 2))

        val observed = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testListAppendEmptyNull() {
        val document = mutableMapOf<String, Any>()
        val docpath = DocumentPath("a.-1")
        val value = null
        val expected = mapOf<String, Any>("a" to listOf<Any>())

        val observed = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testListReplace() {
        val document = mutableMapOf<String, Any>("a" to LinkedList<Any>(listOf<Any>(1, 2, 3)))
        val docpath = DocumentPath("a.0")
        val value = 9
        val expected = mapOf<String, Any>("a" to listOf<Any>(9, 2, 3))

        val observed = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testListDelete() {
        val document = mutableMapOf<String, Any>("a" to LinkedList<Any>(listOf<Any>(1, 2, 3)))
        val docpath = DocumentPath("a.0")
        val value = null
        val expected = mapOf<String, Any>("a" to listOf<Any>(2, 3))

        val observed = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testListReplaceLast() {
        val document = mutableMapOf<String, Any>("a" to LinkedList<Any>(listOf<Any>(5, 4, 3, 2, 1)))
        val docpath = DocumentPath("a.-1")
        val value = 9
        val expected = mapOf<String, Any>("a" to listOf<Any>(5, 4, 3, 2, 9))

        val observed = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testListCreateAndAppend() {
        val document = mutableMapOf<String, Any>()
        val docpath = DocumentPath("a.")
        val value = 1
        val expected = mapOf<String, Any>("a" to listOf<Any>(1))

        val observed = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testListCreateAndAppendEscape() {
        val document = mutableMapOf<String, Any>()
        val docpath = DocumentPath("a\\..")
        val value = 1
        val expected = mapOf<String, Any>("a." to listOf<Any>(1))

        val observed = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testEscapeLastDot() {
        val document = mutableMapOf<String, Any>()
        val docpath = DocumentPath("a\\.")
        val value = 1
        val expected = mapOf<String, Any>("a." to 1)

        val observed = docpath.modifyDocument(document, value)
        Assert.assertEquals(expected, observed)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testLastDotNull() {
        val document = mutableMapOf<String, Any>()
        val docpath = DocumentPath("a.")
        val value = null

        docpath.modifyDocument(document, value)
    }
}
