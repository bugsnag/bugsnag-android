package com.bugsnag.android

import com.bugsnag.android.internal.Journal
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream

class JournalTest {
    @Test
    fun testEmptyEverything() {
        val journal = Journal()
        val document = mutableMapOf<String, Any>()
        val observed = journal.applyTo(document)
        val expected = mutableMapOf<String, Any>()
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testBasicMap() {
        val journal = Journal()
        journal.add(Journal.Command("a", 1))
        val document = mutableMapOf<String, Any>()
        val observed = journal.applyTo(document)
        val expected = mutableMapOf<String, Any>("a" to 1)
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testNonAscii() {
        val journal = Journal()
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
        val journal = Journal()
        journal.add(Journal.Command("int", 1000000000000))
        journal.add(Journal.Command("float", 1.3529104022e80))
        val document = mutableMapOf<String, Any>()
        val observed = journal.applyTo(document)
        val expected = mutableMapOf<String, Any>(
            "int" to 1000000000000,
            "float" to 1.3529104022e80
        )
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testSubmap() {
        val journal = Journal()
        journal.add(Journal.Command("a", mapOf(1 to 2)))
        val document = mutableMapOf<String, Any>()
        val observed = journal.applyTo(document)
        val expected = mutableMapOf<String, Any>("a" to mutableMapOf(1 to 2))
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun testSublist() {
        val journal = Journal()
        journal.add(Journal.Command("a", listOf(1, 2)))
        val document = mutableMapOf<String, Any>()
        val observed = journal.applyTo(document)
        val expected = mutableMapOf<String, Any>("a" to mutableListOf(1, 2))
        Assert.assertEquals(expected, observed)
    }

    @Test
    fun test1EntryJournal() {
        assertSerializeDeserialize(
            "{\"a\":1}\u0000".toByteArray(Charsets.UTF_8),
            Journal.Command("a", 1)
        )
    }

    @Test
    fun test2EntryJournal() {
        assertSerializeDeserialize(
            "{\"a\":1}\u0000{\"b\":\"x\"}\u0000".toByteArray(Charsets.UTF_8),
            Journal.Command("a", 1),
            Journal.Command("b", "x")
        )
    }

    @Test
    fun testRunSerializedJournal() {
        assertRunSerializedJournal(
            initialDocument = mutableMapOf("q" to 100),
            serializedJournal = "{\"a\":1}\u0000{\"b\":\"x\"}\u0000".toByteArray(Charsets.UTF_8),
            expectedDocument = mutableMapOf("q" to 100, "a" to 1, "b" to "x")
        )
    }

    @Test
    fun testRunSerializedJournalWithEscapesDot() {
        assertRunSerializedJournal(
            initialDocument = mutableMapOf("q" to 100),
            serializedJournal = "{\"f\\\\.oo.-1\":1}\u0000{\"b\":\"x\"}\u0000".toByteArray(Charsets.UTF_8),
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
            serializedJournal = "{\"f\\\\\\\\oo.xyz\":1}\u0000{\"b\":\"x\"}\u0000".toByteArray(Charsets.UTF_8),
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
            serializedJournal = "{\"a.b.-1\":1}\u0000{\"b.-1.\":\"x\"}\u0000".toByteArray(Charsets.UTF_8),
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
            "{\"a.\":10false}\u0000".toByteArray(Charsets.UTF_8)
        )
    }

    private fun assertSerializeDeserialize(serialized: ByteArray, vararg entries: Journal.Command) {
        val journal = Journal()
        for (entry in entries) {
            journal.add(entry)
        }
        val expectedBytes = serialized
        val observedBytes = serialize(journal)
        Assert.assertArrayEquals(expectedBytes, observedBytes)

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
        expectedDocument: Any
    ) {
        val journal = deserialize(serializedJournal)
        val observedDocument = journal.applyTo(initialDocument)
        val expectedMap = BugsnagTestUtils.normalized(expectedDocument)
        val observedMap = BugsnagTestUtils.normalized(observedDocument)
        Assert.assertEquals(expectedMap, observedMap)
    }
}
