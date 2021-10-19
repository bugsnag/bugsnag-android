package com.bugsnag.android

import com.bugsnag.android.internal.journal.JournaledDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BugsnagJournalFailedCreationTest {

    @Rule
    @JvmField
    val folder: TemporaryFolder = TemporaryFolder()

    lateinit var baseDocumentPath: File
    lateinit var logger: InterceptingLogger

    @Before
    fun setUp() {
        baseDocumentPath = File(folder.root, "journal")
        logger = InterceptingLogger()
    }

    /**
     * Simulate an I/O error by making the journal files non-writable. The journal should log
     * failure in this case.
     */
    @Test
    fun testFailedJournalCreation() {
        alterJournalFiles(baseDocumentPath, false)

        // attempt to create the journal
        val journalPath = baseDocumentPath
        val journal = BugsnagJournal(logger, journalPath, mapOf())
        assertEquals("Failed to create journal", logger.msg)

        // attempt to add a command
        journal.addCommand("x", 100)

        // attempt to add multiple commands
        journal.addCommands(
            Pair("y", "hello"),
            Pair("z", true)
        )

        // attempt to snapshot the journal
        journal.snapshot()

        // check the journal map is empty
        assertNull(BugsnagJournal.loadPreviousDocument(journalPath))
    }

    /**
     * Simulate an I/O error by making the journal files non-writable. The journal should attempt
     * to recover from this by recreating the stream - using a rate-limit.
     */
    @Test
    fun attemptToRecreateJournal() {
        alterJournalFiles(baseDocumentPath, false)

        // attempt to create the journal
        val journalPath = baseDocumentPath
        val journal = BugsnagJournal(logger, journalPath, mapOf(), 0)
        assertEquals("Failed to create journal", logger.msg)

        // remove the I/O error
        alterJournalFiles(baseDocumentPath, true)

        // attempt to add a command again
        journal.addCommand("x", 100)

        // attempt to add multiple commands
        journal.addCommands(
            Pair("y", "hello"),
            Pair("z", true)
        )

        // attempt to snapshot the journal
        journal.snapshot()

        // check the journal map is empty
        val observed = checkNotNull(BugsnagJournal.loadPreviousDocument(journalPath))
        val expectedDocument = BugsnagJournal.withInitialDocumentContents(
            mapOf(
                "x" to 100L,
                "y" to "hello",
                "z" to true
            )
        )
        assertEquals(expectedDocument["x"], observed["x"])
        assertEquals(expectedDocument["y"], observed["y"])
        assertEquals(expectedDocument["z"], observed["z"])
    }

    private fun alterJournalFiles(baseDocumentPath: File, enabled: Boolean) {
        checkNotNull(baseDocumentPath.parentFile).mkdirs()
        val journalFiles = listOf(
            JournaledDocument.getSnapshotPath(baseDocumentPath),
            JournaledDocument.getRuntimeJournalPath(baseDocumentPath),
            JournaledDocument.getCrashtimeJournalPath(baseDocumentPath)
        )
        journalFiles.forEach { file ->
            file.apply {
                delete()
                createNewFile()
                setWritable(enabled)
                setReadable(enabled)
            }
        }
        folder.root.setWritable(enabled)
    }
}
