package com.bugsnag.android

import com.bugsnag.android.internal.JournaledDocument
import com.bugsnag.android.internal.JsonHelper
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.Thread
import kotlin.concurrent.thread

class JournaledDocumentStressTest {
    @Rule
    @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun testOverflow() {
        val baseDocumentPath = folder.newFile("mydocument")
        val bufferSize = 500L
        val entryCount = 5000
        val expectedDocument = mutableMapOf<String, Any>()
        for (i in 1..entryCount) {
            expectedDocument["a$i"] = "x"
        }
        val document = JournaledDocument(
            baseDocumentPath,
            JournaledDocumentTest.standardType,
            JournaledDocumentTest.standardVersion,
            bufferSize,
            bufferSize,
            mutableMapOf()
        )

        val modifyThread = thread {
            for (i in 1..entryCount) {
                document.addCommand("a$i", "x")
            }
        }

        var value: Any? = null
        var i = 0
        val maxI = entryCount / 10

        while (modifyThread.state != Thread.State.TERMINATED) {
            document.forEach { _ ->
                // Just cycle through all entries
            }
            value = document["a$i"]
            i = (i + 1) % maxI
        }

        // "use" value so that it doesn't get optimized away
        Assert.assertNotNull(value)

        // Leave the OS time to flush the mem mapped file
        Thread.sleep(100)

        assertReloadedDocument(baseDocumentPath, expectedDocument)

        document.close()

        assertSnapshotContents(baseDocumentPath, expectedDocument)
    }

    @Test
    fun testHighWater() {
        val baseDocumentPath = folder.newFile("mydocument")
        val bufferSize = 500L
        val highWater = bufferSize - 100
        val entryCount = 5000
        val expectedDocument = mutableMapOf<String, Any>()
        for (i in 1..entryCount) {
            expectedDocument["a$i"] = "x"
        }
        val document = JournaledDocument(
            baseDocumentPath,
            JournaledDocumentTest.standardType,
            JournaledDocumentTest.standardVersion,
            bufferSize,
            highWater,
            mutableMapOf()
        )

        val modifyThread = thread {
            for (i in 1..entryCount) {
                document.addCommand("a$i", "x")
            }
        }

        thread {
            while (modifyThread.state != Thread.State.TERMINATED) {
                document.snapshotIfHighWater()
                Thread.sleep(10)
            }
        }

        var value: Any? = null
        var i = 0
        val maxI = entryCount / 10

        while (modifyThread.state != Thread.State.TERMINATED) {
            document.forEach { _ ->
                // Just cycle through all entries
            }
            value = document["a$i"]
            i = (i + 1) % maxI
        }

        // "use" value so that it doesn't get optimized away
        Assert.assertNotNull(value)

        // Leave the OS time to flush the mem mapped file
        Thread.sleep(100)

        assertReloadedDocument(baseDocumentPath, expectedDocument)

        document.close()

        assertSnapshotContents(baseDocumentPath, expectedDocument)
    }

    private fun assertReloadedDocument(file: File, expectedDocument: MutableMap<String, Any>) {
        val observedDocument = JournaledDocument.loadDocumentContents(file)
        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(observedDocument)
        )
    }

    private fun assertSnapshotContents(file: File, expectedSnapshot: MutableMap<String, Any>) {
        val snapshotPath = File("${file.path}.snapshot")
        val observedSnapshot = JsonHelper.deserialize(snapshotPath)

        val expectedMap = BugsnagTestUtils.normalized(expectedSnapshot)
        val observedMap = BugsnagTestUtils.normalized(observedSnapshot)
        Assert.assertEquals(expectedMap, observedMap)
    }

    companion object {
        const val clearedByteValue = JournaledDocument.clearedByteValue
    }
}
