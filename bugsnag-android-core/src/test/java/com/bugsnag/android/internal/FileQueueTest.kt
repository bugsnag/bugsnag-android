package com.bugsnag.android.internal

import com.bugsnag.android.EmptyJsonObject
import com.bugsnag.android.EventStore
import com.bugsnag.android.NoopLogger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.UUID

class FileQueueTest {
    private lateinit var dir: File
    private lateinit var fileQueue: FileQueue

    @Before
    fun createFileQueue() {
        dir = Files.createTempDirectory("tmp").toFile()
        dir.deleteRecursively()
        dir.mkdirs()

        fileQueue = FileQueue(
            dir,
            10,
            NoopLogger,
            EventStore.EVENT_COMPARATOR,
            { exception, _, _ -> throw requireNotNull(exception) },
            {}
        )
    }

    @After
    fun cleanup() {
        dir.deleteRecursively()
    }

    @Test
    fun enqueuedFilesAreSorted() {
        val expectedFirst = "1404205127135_683c6b92-b325-4987-80ad-77086509ca1e_startupcrash.json"
        val expectedLast = "1664219155431_042c6195-a32c-2f84-11ae-77086509ca1e_startupcrash.json"

        fileQueue.write(
            "1504255147933_30b7e350-dcd1-4032-969e-98d30be62bbc_startupcrash.json",
            EmptyJsonObject
        )
        fileQueue.write(expectedLast, EmptyJsonObject)
        fileQueue.write(expectedFirst, EmptyJsonObject)

        assertTrue(
            "processLastFile returned false",
            fileQueue.processLastFile { file ->
                assertEquals(expectedLast, file.name)
            }
        )

        assertTrue(
            "processNextFile returned false",
            fileQueue.processNextFile { file ->
                assertEquals(expectedFirst, file.name)
            }
        )
    }

    @Test
    fun flushWithDelete() {
        val filenames = (1..10).map {
            "${it.toString().padStart(4, '0')}_${UUID.randomUUID()}.json"
        }

        filenames.forEach { fileQueue.write(it, EmptyJsonObject) }
        assertFalse(fileQueue.isEmpty())

        var flushedFiles = 0
        fileQueue.processEnqueuedFiles { files ->
            files.forEachIndexed { index, file ->
                val id = file.name.substringBefore('_').toInt()
                assertEquals("files flushed in the wrong order: $file", index + 1, id)
                if (id % 2 == 0) {
                    fileQueue.delete(file)
                }

                flushedFiles++
            }
        }

        assertEquals(filenames.size, flushedFiles)

        val remainingFiles = fileQueue.enqueuedFiles()
        assertEquals(5, remainingFiles.size)
    }

    @Test
    fun limitsStoredFiles() {
        val filenames = (0..100).map {
            "${it.toString().padStart(4, '0')}_${UUID.randomUUID()}.json"
        }

        filenames.forEach { fileQueue.write(it, EmptyJsonObject) }

        val enqueuedFiled = fileQueue.enqueuedFiles()
        assertEquals(10, enqueuedFiled.size)
        assertEquals(filenames.takeLast(10), enqueuedFiled.map { it.name })
    }
}
