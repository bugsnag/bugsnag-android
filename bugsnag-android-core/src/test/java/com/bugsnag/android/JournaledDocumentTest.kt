package com.bugsnag.android

import com.bugsnag.android.internal.journal.Journal
import com.bugsnag.android.internal.journal.JournaledDocument
import com.bugsnag.android.internal.journal.JsonHelper
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class JournaledDocumentTest {
    @Rule
    @JvmField
    val folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun testEmptyDocumentEmptyJournal() {
        val baseDocumentPath = folder.newFile("mydocument")
        val bufferSize = 100L
        val document = JournaledDocument(
            baseDocumentPath,
            standardType,
            standardVersion,
            bufferSize,
            bufferSize,
            mutableMapOf()
        )
        document.close()

        assertReloadedDocument(baseDocumentPath, mutableMapOf())
        assertJournalContents(baseDocumentPath, bufferSize, standardJournalInfoSerialized.toByteArray(Charsets.UTF_8))
        assertSnapshotContents(baseDocumentPath, mutableMapOf())
    }

    @Test
    fun testNonEmptyDocumentEmptyJournal() {
        val baseDocumentPath = folder.newFile("mydocument")
        val bufferSize = 100L
        val document = JournaledDocument(
            baseDocumentPath,
            standardType,
            standardVersion,
            bufferSize,
            bufferSize,
            mutableMapOf("a" to 1)
        )
        document.close()

        assertReloadedDocument(baseDocumentPath, mutableMapOf("a" to 1))
        assertJournalContents(baseDocumentPath, bufferSize, standardJournalInfoSerialized.toByteArray(Charsets.UTF_8))
        assertSnapshotContents(baseDocumentPath, mutableMapOf("a" to 1))
    }

    @Test
    fun testEmptyDocumentNonEmptyJournal() {
        val baseDocumentPath = folder.newFile("mydocument")
        val bufferSize = 100L
        val document = JournaledDocument(
            baseDocumentPath,
            standardType,
            standardVersion,
            bufferSize,
            bufferSize,
            mutableMapOf()
        )
        document.addCommand(Journal.Command("a.-1.b", "test"))

        assertReloadedDocument(
            baseDocumentPath,
            mutableMapOf(
                "a" to mutableListOf(
                    mutableMapOf("b" to "test")
                )
            )
        )
        assertJournalContents(
            baseDocumentPath,
            bufferSize,
            "$standardJournalInfoSerialized{\"a.-1.b\":\"test\"}\u0000".toByteArray(Charsets.UTF_8)
        )
        assertSnapshotContents(baseDocumentPath, mutableMapOf())

        document.close()

        assertReloadedDocument(
            baseDocumentPath,
            mutableMapOf(
                "a" to mutableListOf(
                    mutableMapOf("b" to "test")
                )
            )
        )
        assertJournalContents(
            baseDocumentPath,
            bufferSize,
            standardJournalInfoSerialized.toByteArray(Charsets.UTF_8)
        )
        assertSnapshotContents(
            baseDocumentPath,
            mutableMapOf(
                "a" to mutableListOf(
                    mutableMapOf("b" to "test")
                )
            )
        )
    }

    @Test
    fun testNonEmptyDocumentNonEmptyJournal() {
        val baseDocumentPath = folder.newFile("mydocument")
        val bufferSize = 100L
        val document = JournaledDocument(
            baseDocumentPath,
            standardType,
            standardVersion,
            bufferSize,
            bufferSize,
            mutableMapOf("x" to 9)
        )
        document.addCommand(Journal.Command("a.-1.b", "test"))
        // The in-memory document is updated immediately.
        Assert.assertEquals("test", ((document["a"] as List<*>)[0] as Map<*, *>)["b"])

        assertReloadedDocument(
            baseDocumentPath,
            mutableMapOf(
                "a" to mutableListOf(
                    mutableMapOf("b" to "test")
                ),
                "x" to 9
            )
        )
        assertJournalContents(
            baseDocumentPath,
            bufferSize,
            "$standardJournalInfoSerialized{\"a.-1.b\":\"test\"}\u0000".toByteArray(Charsets.UTF_8)
        )
        assertSnapshotContents(baseDocumentPath, mutableMapOf("x" to 9))

        // The journal is force-flushed when closed or the app terminates for any reason.
        document.close()

        assertReloadedDocument(
            baseDocumentPath,
            mutableMapOf(
                "a" to mutableListOf(
                    mutableMapOf("b" to "test")
                ),
                "x" to 9
            )
        )
        assertJournalContents(
            baseDocumentPath,
            bufferSize,
            standardJournalInfoSerialized.toByteArray(Charsets.UTF_8)
        )
        assertSnapshotContents(
            baseDocumentPath,
            mutableMapOf(
                "a" to mutableListOf(
                    mutableMapOf("b" to "test")
                ),
                "x" to 9
            )
        )
    }

    @Test
    fun testOverflow() {
        val baseDocumentPath = folder.newFile("mydocument")
        val bufferSize = standardJournalInfoSerialized.length + 10L
        val document = JournaledDocument(
            baseDocumentPath,
            standardType,
            standardVersion,
            bufferSize,
            bufferSize,
            mutableMapOf("x" to 9)
        )
        document.addCommand(Journal.Command("a", 1))
        document.addCommand("b", 2)

        assertReloadedDocument(
            baseDocumentPath,
            mutableMapOf(
                "a" to 1,
                "b" to 2,
                "x" to 9
            )
        )
        assertJournalContents(
            baseDocumentPath,
            bufferSize,
            "$standardJournalInfoSerialized{\"b\":2}\u0000".toByteArray(Charsets.UTF_8)
        )
        assertSnapshotContents(
            baseDocumentPath,
            mutableMapOf(
                "a" to 1,
                "x" to 9
            )
        )

        document.close()

        assertReloadedDocument(
            baseDocumentPath,
            mutableMapOf(
                "a" to 1,
                "b" to 2,
                "x" to 9
            )
        )
        assertJournalContents(
            baseDocumentPath,
            bufferSize,
            standardJournalInfoSerialized.toByteArray(Charsets.UTF_8)
        )
        assertSnapshotContents(
            baseDocumentPath,
            mutableMapOf(
                "a" to 1,
                "b" to 2,
                "x" to 9
            )
        )
    }

    @Test
    fun testHighWater() {
        val baseDocumentPath = folder.newFile("mydocument")
        val bufferSize = standardJournalInfoSerialized.length + 20L
        val document = JournaledDocument(
            baseDocumentPath,
            standardType,
            standardVersion,
            bufferSize,
            bufferSize - 10,
            mutableMapOf("x" to 9)
        )
        document.addCommand(Journal.Command("a", 1))
        document.addCommand(Journal.Command("b", 2))

        assertJournalContents(
            baseDocumentPath,
            bufferSize,
            "$standardJournalInfoSerialized{\"a\":1}\u0000{\"b\":2}\u0000".toByteArray(Charsets.UTF_8)
        )
        assertSnapshotContents(
            baseDocumentPath,
            mutableMapOf(
                "x" to 9
            )
        )
        assertReloadedDocument(
            baseDocumentPath,
            mutableMapOf(
                "a" to 1,
                "b" to 2,
                "x" to 9
            )
        )

        document.snapshotIfHighWater()

        assertJournalContents(
            baseDocumentPath,
            bufferSize,
            standardJournalInfoSerialized.toByteArray(Charsets.UTF_8)
        )
        assertSnapshotContents(
            baseDocumentPath,
            mutableMapOf(
                "a" to 1,
                "b" to 2,
                "x" to 9
            )
        )
        assertReloadedDocument(
            baseDocumentPath,
            mutableMapOf(
                "a" to 1,
                "b" to 2,
                "x" to 9
            )
        )

        document.close()

        assertJournalContents(
            baseDocumentPath,
            bufferSize,
            standardJournalInfoSerialized.toByteArray(Charsets.UTF_8)
        )
        assertSnapshotContents(
            baseDocumentPath,
            mutableMapOf(
                "a" to 1,
                "b" to 2,
                "x" to 9
            )
        )
        assertReloadedDocument(
            baseDocumentPath,
            mutableMapOf(
                "a" to 1,
                "b" to 2,
                "x" to 9
            )
        )
    }

    @Test(expected = IllegalStateException::class)
    fun testModifyAfterClose() {
        val baseDocumentPath = folder.newFile("mydocument")
        val bufferSize = standardJournalInfoSerialized.length + 10L
        val document = JournaledDocument(
            baseDocumentPath,
            standardType,
            standardVersion,
            bufferSize,
            bufferSize,
            mutableMapOf("x" to 9)
        )
        document.addCommand(Journal.Command("a", 1))
        document.addCommand(Journal.Command("b", 2))
        document.close()
        document.addCommand(Journal.Command("c", 3))
    }

    @Test(expected = IllegalStateException::class)
    fun testSnapshotAfterClose() {
        val baseDocumentPath = folder.newFile("mydocument")
        val bufferSize = standardJournalInfoSerialized.length + 10L
        val document = JournaledDocument(
            baseDocumentPath,
            standardType,
            standardVersion,
            bufferSize,
            bufferSize,
            mutableMapOf("x" to 9)
        )
        document.addCommand(Journal.Command("a", 1))
        document.addCommand(Journal.Command("b", 2))
        document.close()
        document.snapshot()
    }

    @Test
    fun testAddMultiple() {
        val baseDocumentPath = folder.newFile("mydocument")
        val bufferSize = 100L
        val document = JournaledDocument(
            baseDocumentPath,
            standardType,
            standardVersion,
            bufferSize,
            bufferSize,
            mapOf("a" to 1)
        )
        document.addCommands(
            Pair("x", 1),
            Pair("y", 100),
            Pair("z", 10000)
        )
        document.close()

        val expectedDocument = mapOf<String, Any>(
            "a" to 1,
            "x" to 1,
            "y" to 100,
            "z" to 10000
        )
        assertReloadedDocument(baseDocumentPath, expectedDocument)
        assertJournalContents(baseDocumentPath, bufferSize, standardJournalInfoSerialized.toByteArray(Charsets.UTF_8))
        assertSnapshotContents(baseDocumentPath, expectedDocument)
    }

    private fun assertReloadedDocument(file: File, expectedDocument: Map<String, Any>) {
        val observedDocument = JournaledDocument.loadDocumentContents(file)
        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(observedDocument)
        )
    }

    private fun assertFileContents(file: File, expectedContents: ByteArray) {
        val observedContents = file.readBytes()
        Assert.assertArrayEquals(expectedContents, observedContents)
    }

    private fun assertJournalContents(file: File, size: Long, expected: ByteArray) {
        val expectedContents = ByteArray(size.toInt())
        expectedContents.fill(clearedByteValue)
        expected.copyInto(expectedContents)
        val journalPath = File("${file.path}.journal")
        assertFileContents(journalPath, expectedContents)
    }

    private fun assertSnapshotContents(file: File, expectedSnapshot: Map<String, Any>) {
        val snapshotPath = File("${file.path}.snapshot")
        val observedSnapshot = JsonHelper.deserialize(snapshotPath)

        val expectedMap = BugsnagTestUtils.normalized(expectedSnapshot)
        val observedMap = BugsnagTestUtils.normalized(observedSnapshot)
        Assert.assertEquals(expectedMap, observedMap)
    }

    companion object {
        const val standardType = "Bugsnag state"
        const val standardVersion = 1
        const val clearedByteValue = JournaledDocument.clearedByteValue
        const val standardJournalInfoSerialized = "{\"*\":{\"type\":\"Bugsnag state\",\"version\":1}}\u0000"
    }
}
