package com.bugsnag.android.ndk

import com.bugsnag.android.ndk.OpaqueValue.Companion.makeSafe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class OpaqueValueTest {

    @Test
    fun testBooleanOpaqueValue() {
        val result = makeSafe(true)
        assertEquals(true, result)
        val result2 = makeSafe(false)
        assertEquals(false, result2)
    }

    @Test
    fun testNumberOpaqueValue() {
        val result = makeSafe(1.5)
        assertEquals(1.5, result)
    }

    @Test
    fun testValidStringOpaqueValue() {
        val result = makeSafe("Test")
        assertEquals("Test", result)
    }

    @Test
    fun testInvalidStringOpaqueValue() {
        val testObject = "TestTestTestTestTestTestTestTestTestTestTestTestTestTestTestTestTestTestTestTest"
        val result = makeSafe(testObject)
        assertTrue(result is OpaqueValue)
    }

    @Test
    fun testCollectionOpaqueValue() {
        val testObject = listOf("Test1", "Test2")
        val result = makeSafe(testObject)
        assertTrue(result is OpaqueValue)
    }

    @Test
    fun testMapOpaqueValue() {
        val testObject = mapOf(1 to "Test1")
        val result = makeSafe(testObject)
        assertTrue(result is OpaqueValue)
    }

    @Test
    fun testArrayOpaqueValue() {
        val testObject = arrayOf("Test1", "Test2")
        val result = makeSafe(testObject)
        assertTrue(result is OpaqueValue)
    }
}
