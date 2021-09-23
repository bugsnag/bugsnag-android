package com.bugsnag.android

import com.bugsnag.android.internal.journal.JournalKeys
import org.junit.Assert
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BugsnagJournalTest {
    companion object {
        @ClassRule
        @JvmField
        val folder: TemporaryFolder = TemporaryFolder()
    }
    private val baseDocumentPath by lazy { File(folder.root, "mydocument") }

    @Test
    fun testNoOldDocument() {
        val oldDocument = BugsnagJournal.loadPreviousDocument(File(baseDocumentPath, "doesNotExistDocument"))
        Assert.assertNull(oldDocument)
    }

    @Test
    fun testInitialDocumentContents() {
        val observedDocument = BugsnagJournal.withInitialDocumentContents(mapOf())
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
        val journal = BugsnagJournal(NoopLogger, baseDocumentPath, mapOf())
        journal.snapshot()

        val observedDocument = BugsnagJournal.loadPreviousDocument(baseDocumentPath)
        val expectedDocument = BugsnagJournal.withInitialDocumentContents(mapOf())

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
        val journal = BugsnagJournal(NoopLogger, baseDocumentPath, mapOf())
        journal.addCommand("x", 100)
        journal.snapshot()

        val observedDocument = BugsnagJournal.loadPreviousDocument(baseDocumentPath)
        val expectedDocument = HashMap(BugsnagJournal.withInitialDocumentContents(mapOf()))
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
        val journal = BugsnagJournal(NoopLogger, baseDocumentPath, mapOf())
        journal.addCommand("one hundred", 100)
        journal.addCommand("""three\.four\.five\.slash\\slash.-1""", "x")
        journal.addCommand("x.-1.x", listOf(1, "foo"))
        journal.addCommand("x.-1.y", mapOf("a" to "b"))
        journal.snapshot()

        val observedDocument = BugsnagJournal.loadPreviousDocument(baseDocumentPath)
        val expectedDocument = BugsnagJournal.withInitialDocumentContents(
            mapOf(
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
        val journal = BugsnagJournal(NoopLogger, baseDocumentPath, mapOf())
        journal.addCommands(
            Pair("one hundred", 100),
            Pair("""three\.four\.five\.slash\\slash.-1""", "x"),
            Pair("x.-1.x", listOf(1, "foo")),
            Pair("x.-1.y", mapOf("a" to "b"))
        )
        journal.snapshot()

        val observedDocument = BugsnagJournal.loadPreviousDocument(baseDocumentPath)
        val expectedDocument = BugsnagJournal.withInitialDocumentContents(
            mapOf(
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
}
