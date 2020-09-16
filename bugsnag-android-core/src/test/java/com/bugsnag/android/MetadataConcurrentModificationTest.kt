package com.bugsnag.android

import org.junit.Assert.assertNotNull
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
}
