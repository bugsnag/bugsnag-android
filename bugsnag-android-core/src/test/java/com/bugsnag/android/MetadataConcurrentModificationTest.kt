package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Test
import java.util.concurrent.Executors
import java.util.regex.Pattern

internal class MetadataConcurrentModificationTest {

    @Test()
    fun testHandlesConcurrentModification() {
        val metadata = Metadata().copy()
        val executor = Executors.newSingleThreadExecutor()

        repeat(100) { count ->
            assertNotNull(metadata.toMap())
            executor.execute {
                metadata.store["$count"] = mutableMapOf<String, Any>(Pair("$count", count))
            }
        }
    }

    /**
     * Regression unit test that verifies setting redactedKeys
     * concurrently on [Metadata] does not throw a ConcurrentModificationException
     */
    @Test()
    fun testConcurrentModificationRedactedKeys() {
        val keys = mutableSetOf(Pattern.compile(".*alpha.*"), Pattern.compile(".*omega.*"))
        val orig = Metadata()
        val executor = Executors.newSingleThreadExecutor()

        repeat(100) {
            orig.redactedKeys = keys
            executor.execute {
                keys.add(Pattern.compile(".*$it.*"))
            }
        }
    }

    @Test()
    fun testRedactedKeysCopy() {
        val orig = Metadata()
        val keys = mutableSetOf(Pattern.compile(".*alpha.*"), Pattern.compile(".*omega.*"))
        orig.redactedKeys = keys
        val copy = orig.copy()
        assertNotSame(orig.redactedKeys, copy.redactedKeys)
        assertEquals(keys, copy.redactedKeys)
    }
}
