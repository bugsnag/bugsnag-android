package com.bugsnag.android

import com.bugsnag.android.internal.journal.Journal
import com.bugsnag.android.internal.journal.JournalKeys
import com.bugsnag.android.internal.journal.JournaledDocument
import org.junit.Assert
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger

class BugsnagJournalTest {
    companion object {
        @ClassRule
        @JvmField
        val folder: TemporaryFolder = TemporaryFolder()
        internal val counter = AtomicInteger(1)
    }
    private val baseDocumentPath by lazy { File(folder.root, "mydocument") }

    fun newBaseDocumentPath(): File {
        return File(baseDocumentPath.toString() + counter.getAndIncrement())
    }

    @Test
    fun testNoOldDocument() {
        val oldDocument = BugsnagJournal.loadPreviousDocument(File(baseDocumentPath, "doesNotExistDocument"))
        Assert.assertNull(oldDocument)
    }

    @Test
    fun testInitialDocumentContents() {
        val observedDocument = BugsnagJournal.withInitialDocumentContents(mutableMapOf())
        val expectedDocument = mapOf<String, Any>(
            JournalKeys.keyVersionInfo to mapOf(
                JournalKeys.keyType to BugsnagJournal.journalType,
                JournalKeys.keyVersion to BugsnagJournal.journalVersion
            )
        )
        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(observedDocument)
        )
    }

    @Test
    fun testEmptyDocument() {
        val journalPath = newBaseDocumentPath()
        val journal = BugsnagJournal(NoopLogger, journalPath, mutableMapOf())
        journal.snapshot()

        val observedDocument = BugsnagJournal.loadPreviousDocument(journalPath)
        val expectedDocument = BugsnagJournal.withInitialDocumentContents(mutableMapOf())

        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(observedDocument)
        )
        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(journal.document)
        )
    }

    @Test
    fun testJournalEntry() {
        val journalPath = newBaseDocumentPath()
        val journal = BugsnagJournal(NoopLogger, journalPath, mutableMapOf())
        journal.addCommand("x", 100)
        journal.snapshot()

        val observedDocument = BugsnagJournal.loadPreviousDocument(journalPath)
        val expectedDocument = HashMap(BugsnagJournal.withInitialDocumentContents(mutableMapOf()))
        expectedDocument["x"] = 100

        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(observedDocument)
        )
        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(journal.document)
        )
    }

    @Test
    fun testManyJournalEntries() {
        val journalPath = newBaseDocumentPath()
        val journal = BugsnagJournal(NoopLogger, journalPath, mutableMapOf())
        journal.addCommand("one hundred", 100)
        journal.addCommand("""three\.four\.five\.slash\\slash.-1""", "x")
        journal.addCommand("x.-1.x", mutableListOf(1, "foo"))
        journal.addCommand("x.-1.y", mutableMapOf("a" to "b"))
        journal.snapshot()

        val observedDocument = BugsnagJournal.loadPreviousDocument(journalPath)
        val expectedDocument = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "one hundred" to 100,
                """three.four.five.slash\slash""" to listOf("x"),
                "x" to listOf(
                    mapOf(
                        "x" to listOf(1, "foo"),
                        "y" to mapOf(
                            "a" to "b"
                        )
                    )
                )
            )
        )

        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(observedDocument)
        )
        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(journal.document)
        )
    }

    @Test
    fun testMultipleJournalEntries() {
        val journalPath = newBaseDocumentPath()
        val journal = BugsnagJournal(NoopLogger, journalPath, mutableMapOf())
        journal.addCommands(
            Pair("one hundred", 100),
            Pair("""three\.four\.five\.slash\\slash.-1""", "x"),
            Pair("x.-1.x", mutableListOf(1, "foo")),
            Pair("x.-1.y", mutableMapOf("a" to "b"))
        )
        journal.snapshot()

        val observedDocument = BugsnagJournal.loadPreviousDocument(journalPath)
        val expectedDocument = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "one hundred" to 100,
                """three.four.five.slash\slash""" to listOf("x"),
                "x" to listOf(
                    mapOf(
                        "x" to listOf(1, "foo"),
                        "y" to mapOf(
                            "a" to "b"
                        )
                    )
                )
            )
        )

        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(observedDocument)
        )
        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(journal.document)
        )
    }

    @Test
    fun testCrashtimeJournal() {
        val journalPath = newBaseDocumentPath()
        val runtimeJournal = BugsnagJournal(NoopLogger, journalPath, mutableMapOf())
        runtimeJournal.addCommand("one hundred", 100)

        val crashtimeJournal = Journal(BugsnagJournal.journalType, BugsnagJournal.journalVersion)
        crashtimeJournal.add(Journal.Command("a", "a"))
        crashtimeJournal.add(Journal.Command("b", 60))
        crashtimeJournal.serialize(FileOutputStream(JournaledDocument.getCrashtimeJournalPath(journalPath)))

        val observedDocument = BugsnagJournal.loadPreviousDocument(journalPath)
        val expectedDocument = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "one hundred" to 100,
                "a" to "a",
                "b" to 60
            )
        )

        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(observedDocument)
        )
    }

    @Test
    fun testUnspecialMapPath() {
        Assert.assertEquals("""""", BugsnagJournal.unspecialMapPath(""""""))
        Assert.assertEquals("""a""", BugsnagJournal.unspecialMapPath("""a"""))
        Assert.assertEquals("""\0""", BugsnagJournal.unspecialMapPath("""0"""))
        Assert.assertEquals("""\1234""", BugsnagJournal.unspecialMapPath("""1234"""))
        Assert.assertEquals("""a\\\.\+""", BugsnagJournal.unspecialMapPath("""a\.+"""))
    }

    @Test
    fun testJournalEntryUnspecialMapPath() {
        val journalPath = newBaseDocumentPath()
        val journal = BugsnagJournal(NoopLogger, journalPath, mutableMapOf())
        journal.addCommand(BugsnagJournal.unspecialMapPath("""a\.+"""), 100)
        journal.snapshot()

        val observedDocument = BugsnagJournal.loadPreviousDocument(journalPath)
        val expectedDocument = HashMap(BugsnagJournal.withInitialDocumentContents(mutableMapOf()))
        expectedDocument["""a\.+"""] = 100

        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(observedDocument)
        )
        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(journal.document)
        )
    }
}
