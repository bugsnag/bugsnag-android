package com.bugsnag.android.internal.journal

import com.bugsnag.android.NoopLogger
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

private const val FIRST_MOCK_MS = 12345670000L

class BugsnagJournalStoreTest {

    @Rule
    @JvmField
    val folder: TemporaryFolder = TemporaryFolder()

    lateinit var storageDir: File

    @Before
    fun setUp() {
        storageDir = File(folder.root, "bugsnag-dir")
        storageDir.mkdirs()
    }

    /**
     * Creating a new journal creates the necessary files on disk.
     */
    @Test
    fun createNewJournal() {
        val store = BugsnagJournalStore(storageDir, NoopLogger) { FIRST_MOCK_MS }
        assertArrayEquals(emptyArray<File>(), storageDir.listFiles())
        store.createNewJournal()
        assertCurrentJournalFilesPresent()
    }

    /**
     * findOldJournalFiles() handles an empty list.
     */
    @Test
    fun testFindOldFilesEmpty() {
        val store = BugsnagJournalStore(storageDir, NoopLogger) { FIRST_MOCK_MS }
        assertTrue(store.findOldJournalFiles().isEmpty())
    }

    /**
     * findOldJournalFiles() sorts and filters for .journal files specifically.
     */
    @Test
    fun testFindOldFiles() {
        listOf(
            "journal_150000000.journal",
            "journal_190000100.journal",
            "journal_190000201.snapshot",
            "journal_170000000.journal.crashtime",
            "journal_180002000.journal",
            "journal_160005000.journal",
            "journal_170003000.journal",
            "journal_190000201.snapshot.new"
        ).forEach {
            File(storageDir, it).createNewFile()
        }

        val store = BugsnagJournalStore(storageDir, NoopLogger) { FIRST_MOCK_MS }
        val observed = store.findOldJournalFiles().map { it.name }
        assertEquals(5, observed.size)
        val expected = listOf(
            "journal_190000100.journal",
            "journal_180002000.journal",
            "journal_170003000.journal",
            "journal_160005000.journal",
            "journal_150000000.journal"
        )
        assertEquals(expected, observed)
    }

    /**
     * Checks that the action is not invoked for invalid files, as an Event cannot be generated
     * from these.
     */
    @Test
    fun testProcessInvalidFiles() {
        listOf(
            "journal_150000000.journal",
            "journal_190000201.journal",
            "journal_190000201.snapshot",
            "journal_190000201.journal.crashtime",
            "journal_180002000.journal",
            "journal_160005000.journal",
            "journal_170003000.journal",
            "journal_190000201.snapshot.new"
        ).forEach {
            File(storageDir, it).createNewFile()
        }

        val store = BugsnagJournalStore(storageDir, NoopLogger) { FIRST_MOCK_MS }.apply {
            createNewJournal()
        }

        var invoked = false
        store.processPreviousJournals {
            invoked = true
        }

        // check that 0 valid events were generated
        assertFalse(invoked)

        // check that only the current journal is present
        assertCurrentJournalFilesPresent()
    }

    /**
     * Checks that the action is only invoked for the most recent journal.
     */
    @Test
    fun testProcessMostRecentJournal() {
        val initialFiles = listOf(
            "journal_150000000.journal",
            "journal_190000201.journal",
            "journal_190000201.snapshot",
            "journal_190000201.journal.crashtime",
            "journal_180002000.journal",
            "journal_160005000.journal",
            "journal_200003000.journal",
            "journal_190000201.snapshot.new"
        )
        initialFiles.forEach {
            File(storageDir, it).createNewFile()
        }

        BugsnagJournalStore(storageDir, NoopLogger) { FIRST_MOCK_MS }.apply {
            createNewJournal()
            processMostRecentJournal { }
        }

        // check that only the most recent journal was processed
        val expected = initialFiles.minus("journal_200003000.journal").plus(
            listOf("journal_12345670000.journal", "journal_12345670000.snapshot")
        ).sorted()
        val observed = checkNotNull(storageDir.listFiles()).map { it.name }.sorted().toList()
        assertEquals(expected, observed)
    }

    /**
     * The current journal is not deleted when previous journals are processed
     */
    @Test
    fun currentJournalNotDeleted() {
        // create 2 bugsnag journals, one after another
        BugsnagJournalStore(storageDir, NoopLogger) { FIRST_MOCK_MS }.apply {
            createNewJournal()
            processPreviousJournals { }
        }

        // assert that a journal was created for each
        assertCurrentJournalFilesPresent()
    }

    private fun assertCurrentJournalFilesPresent() {
        val observed = checkNotNull(storageDir.listFiles()).sorted().map { it.name }
        val expected = listOf("journal_$FIRST_MOCK_MS.journal", "journal_$FIRST_MOCK_MS.snapshot")
        assertEquals(expected, observed)
    }
}
