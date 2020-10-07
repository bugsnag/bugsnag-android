package com.bugsnag.android

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.concurrent.Executors

internal class MetadataConcurrentModificationTest {

    @Test()
    fun testHandlesConcurrentModification() {
        val metadata = Metadata().copy()
        val executor = Executors.newSingleThreadExecutor()

        repeat(100) { count ->
            assertNotNull(metadata.toMap())
            executor.execute {
                metadata.store["$count"] = count
            }
        }
    }

    @Test()
    fun testConcurrentModificationRedactedKeys() {
        val keys = mutableSetOf("alpha", "omega")
        val orig = Metadata()
        val executor = Executors.newSingleThreadExecutor()

        repeat(100) {
            orig.redactedKeys = keys
            executor.execute {
                keys.add("$it")
            }
        }
    }

    @Test()
    fun testRedactedKeysCopy() {
        val orig = Metadata()
        orig.redactedKeys = mutableSetOf("alpha", "omega")
        val copy = orig.copy()
        assertSame(orig.redactedKeys, copy.redactedKeys)
    }
}
