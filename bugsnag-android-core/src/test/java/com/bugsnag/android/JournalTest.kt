package com.bugsnag.android

import com.bugsnag.android.internal.Journal
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream

class JournalTest {
    @Test
    fun testEmptyEverything() {
        val journal = Journal(standardType, standardVersion)
        val document = mutableMapOf<String, Any>()
        val observed = journal.applyTo(document)
        val expected = mutableMapOf<String, Any>()
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testBasicMap() {
        val journal = Journal(standardType, standardVersion)
        journal.add(Journal.Command("a", 1))
        val document = mutableMapOf<String, Any>()
        val observed = journal.applyTo(document)
        val expected = mutableMapOf<String, Any>("a" to 1)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testNonAscii() {
        val journal = Journal(standardType, standardVersion)
        journal.add(Journal.Command("猫", "cat (Japanese)"))
        journal.add(Journal.Command("பூனை", "cat (Tamil)"))
        val document = mutableMapOf<String, Any>()
        val observed = journal.applyTo(document)
        val expected = mutableMapOf<String, Any>(
            "猫" to "cat (Japanese)",
            "பூனை" to "cat (Tamil)"
        )
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testLargeValues() {
        val journal = Journal(standardType, standardVersion)
        journal.add(Journal.Command("int", 1000000000000))
        journal.add(Journal.Command("float", 1.3529104022e80))
        val document = mutableMapOf<String, Any>()
        val observed = journal.applyTo(document)
        val expected = mutableMapOf(
            "int" to 1000000000000,
            "float" to 1.3529104022e80
        )
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testSubmap() {
        val journal = Journal(standardType, standardVersion)
        journal.add(Journal.Command("a", mapOf(1 to 2)))
        val document = mutableMapOf<String, Any>()
        val observed = journal.applyTo(document)
        val expected = mutableMapOf<String, Any>("a" to mutableMapOf(1 to 2))
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testSublist() {
        val journal = Journal(standardType, standardVersion)
        journal.add(Journal.Command("a", listOf(1, 2)))
        val document = mutableMapOf<String, Any>()
        val observed = journal.applyTo(document)
        val expected = mutableMapOf<String, Any>("a" to mutableListOf(1, 2))
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun test1EntryJournal() {
        assertSerializeDeserialize(
            (standardJournalInfoSerialized + "{\"a\":1}\u0000").toByteArray(Charsets.UTF_8),
            Journal.Command("a", 1)
        )
    }

    @Test
    fun test2EntryJournal() {
        assertSerializeDeserialize(
            (standardJournalInfoSerialized + "{\"a\":1}\u0000{\"b\":\"x\"}\u0000").toByteArray(Charsets.UTF_8),
            Journal.Command("a", 1),
            Journal.Command("b", "x")
        )
    }

    @Test
    fun testRunSerializedJournal() {
        assertRunSerializedJournal(
            initialDocument = mutableMapOf("q" to 100),
            serializedJournal = (standardJournalInfoSerialized + "{\"a\":1}\u0000{\"b\":\"x\"}\u0000")
                .toByteArray(Charsets.UTF_8),
            expectedDocument = mutableMapOf("q" to 100, "a" to 1, "b" to "x")
        )
    }

    @Test
    fun testRunSerializedJournalWithEscapesDot() {
        assertRunSerializedJournal(
            initialDocument = mutableMapOf("q" to 100),
            serializedJournal = (standardJournalInfoSerialized + "{\"f\\\\.oo.-1\":1}\u0000{\"b\":\"x\"}\u0000")
                .toByteArray(Charsets.UTF_8),
            expectedDocument = mutableMapOf(
                "q" to 100,
                "f.oo" to mutableListOf(1),
                "b" to "x"
            )
        )
    }

    @Test
    fun testRunSerializedJournalWithEscapesBackslash() {
        assertRunSerializedJournal(
            initialDocument = mutableMapOf("q" to 100),
            serializedJournal = (standardJournalInfoSerialized + "{\"f\\\\\\\\oo.xyz\":1}\u0000{\"b\":\"x\"}\u0000")
                .toByteArray(Charsets.UTF_8),
            expectedDocument = mutableMapOf(
                "q" to 100,
                "f\\oo" to mutableMapOf("xyz" to 1),
                "b" to "x"
            )
        )
    }

    @Test
    fun testRunSerializedJournalDeep() {
        assertRunSerializedJournal(
            initialDocument = mutableMapOf("q" to 100),
            serializedJournal = (standardJournalInfoSerialized + "{\"a.b.-1\":1}\u0000{\"b.-1.\":\"x\"}\u0000")
                .toByteArray(Charsets.UTF_8),
            expectedDocument = mutableMapOf(
                "q" to 100,
                "a" to mapOf<String, Any>("b" to listOf<Any>(1L)),
                "b" to listOf<Any>(listOf<Any>("x"))
            )
        )
    }

    @Test(expected = IllegalStateException::class)
    fun testDeserializeFail() {
        deserialize(
            (standardJournalInfoSerialized + "{\"a.\":10false}\u0000").toByteArray(Charsets.UTF_8)
        )
    }

    @Test
    fun testDeserializeEmpty() {
        deserialize(
            (
                "{\"*\":{\"type\":\"Bugsnag state\",\"version\":1}}\u0000"
                ).toByteArray(Charsets.UTF_8)
        )
    }

    @Test
    fun testDeserializeBasic() {
        deserialize(
            (
                "{\"*\":{\"type\":\"Bugsnag state\",\"version\":1}}\u0000" +
                    "{\"a\":10}\u0000"
                ).toByteArray(Charsets.UTF_8)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDeserializeMissingType() {
        deserialize(
            (
                "{\"*\":{\"version\":1}}\u0000" +
                    "{\"a\":10}\u0000"
                ).toByteArray(Charsets.UTF_8)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDeserializeMissingVersion() {
        deserialize(
            (
                "{\"*\":{\"type\":\"Bugsnag state\"}}\u0000" +
                    "{\"a\":10}\u0000"
                ).toByteArray(Charsets.UTF_8)
        )
    }

    private fun assertSerializeDeserialize(serialized: ByteArray, vararg entries: Journal.Command) {
        val journal = Journal(standardType, standardVersion)
        for (entry in entries) {
            journal.add(entry)
        }
        val expectedAsString = serialized.toString(Charsets.UTF_8)
        val observedAsString = serialize(journal).toString(Charsets.UTF_8)
        Assert.assertEquals(expectedAsString, observedAsString)

        val expectedEntry = journal
        val observedEntry = deserialize(serialized)
        assertEquivalent(expectedEntry, observedEntry)
    }

    private fun assertEquivalent(expected: Journal, observed: Journal) {
        val expectedBytes = serialize(expected)
        val observedBytes = serialize(observed)
        Assert.assertArrayEquals(expectedBytes, observedBytes)
    }

    fun serialize(journal: Journal): ByteArray {
        val outs = ByteArrayOutputStream()
        journal.serialize(outs)
        return outs.toByteArray()
    }

    fun deserialize(bytes: ByteArray): Journal {
        return Journal.deserialize(bytes)
    }

    private fun assertRunSerializedJournal(
        initialDocument: MutableMap<String, Any>,
        serializedJournal: ByteArray,
        expectedDocument: MutableMap<String, Any>
    ) {
        val journal = deserialize(serializedJournal)
        val observedDocument = journal.applyTo(initialDocument)
        val expectedMap = BugsnagTestUtils.normalized(expectedDocument)
        val observedMap = BugsnagTestUtils.normalized(observedDocument)
        Assert.assertEquals(expectedMap, observedMap)
    }

    companion object {
        const val standardType = "Bugsnag state"
        const val standardVersion = 1
        const val standardJournalInfoSerialized = "{\"*\":{\"type\":\"Bugsnag state\",\"version\":1}}\u0000"
    }
}
