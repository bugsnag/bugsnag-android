package com.bugsnag.android

import org.junit.Assert
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BugsnagMainJournalTest {
    companion object {
        @ClassRule
        @JvmField
        val folder: TemporaryFolder = TemporaryFolder()
    }
    private val baseDocumentPath by lazy { File(folder.root, "mydocument") }

    // Simulate a fresh journal since we can't actually reinitialize it.
    private fun beginClean(): MutableMap<in String, out Any>? {
        val oldContents = BugsnagMainJournal.startInstance(NoopLogger, baseDocumentPath, mapOf())
        BugsnagMainJournal.instance.journal.addCommand("", BugsnagMainJournal.initialDocumentContents(mapOf()))
        BugsnagMainJournal.instance.snapshot()
        return oldContents
    }

    private fun loadFromDisk(): MutableMap<in String, out Any> {
        return BugsnagMainJournal.loadPrevious(baseDocumentPath)
    }

    @Test
    fun testOldDocumentMustRunFirst() {
        // We can only test that the first run returns null since we can only initialize once.
        // Testing for existing journal contents requires E2E tests.
        val oldDocument = beginClean()
        Assert.assertNull(oldDocument)
    }

    @Test
    fun testInitialDocumentContents() {
        val observedDocument = BugsnagMainJournal.initialDocumentContents(mapOf())
        val expectedDocument = mapOf<String, Any>(
            BugsnagMainJournal.versionInfoKey to mapOf(
                BugsnagMainJournal.journalTypeKey to BugsnagMainJournal.journalType,
                BugsnagMainJournal.journalVersionKey to BugsnagMainJournal.journalVersion
            )
        )
        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(observedDocument)
        )
    }

    @Test
    fun testEmptyDocument() {
        beginClean()

        BugsnagMainJournal.instance.snapshot()
        val observedDocument = loadFromDisk()
        val expectedDocument = BugsnagMainJournal.initialDocumentContents(mapOf())

        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(observedDocument)
        )
        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(BugsnagMainJournal.instance.document)
        )
    }

    @Test
    fun testJournalEntry() {
        beginClean()
        BugsnagMainJournal.instance.addCommand("x", 100)

        BugsnagMainJournal.instance.snapshot()
        val observedDocument = loadFromDisk()
        val expectedDocument = HashMap(BugsnagMainJournal.initialDocumentContents(mapOf()))
        expectedDocument["x"] = 100

        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(observedDocument)
        )
        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(BugsnagMainJournal.instance.document)
        )
    }

    @Test
    fun testManyJournalEntries() {
        beginClean()
        BugsnagMainJournal.instance.addCommand("one hundred", 100)
        BugsnagMainJournal.instance.addCommand("three\\.four\\.five\\.slash\\\\slash.-1", "x")
        BugsnagMainJournal.instance.addCommand("x.-1.x", listOf(1, "foo"))
        BugsnagMainJournal.instance.addCommand("x.-1.y", mapOf("a" to "b"))

        BugsnagMainJournal.instance.snapshot()
        val observedDocument = loadFromDisk()
        val expectedDocument = HashMap(BugsnagMainJournal.initialDocumentContents(mapOf()))
        expectedDocument["one hundred"] = 100
        expectedDocument["three.four.five.slash\\slash"] = listOf(
            "x"
        )
        expectedDocument["x"] = listOf(
            mapOf(
                "x" to listOf(1, "foo"),
                "y" to mapOf(
                    "a" to "b"
                )
            )
        )

        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(observedDocument)
        )
        Assert.assertEquals(
            BugsnagTestUtils.normalized(expectedDocument),
            BugsnagTestUtils.normalized(BugsnagMainJournal.instance.document)
        )
    }
}
