package com.bugsnag.android

import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream

class JournalTest {
    @Test
    fun testEmptyEverything() {
        val expected = mutableMapOf<String, Any>()
        val journal = Journal()
        val document = mutableMapOf<String, Any>()
        val actual = journal.applyTo(document)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testBasicMap() {
        val expected = mutableMapOf<String, Any>("a" to 1)
        val journal = Journal()
        journal.add(Journal.Command("a", 1))
        val document = mutableMapOf<String, Any>()
        val actual = journal.applyTo(document)
        Assert.assertEquals(expected, actual)
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
            mutableMapOf("q" to 100),
            "{\"a\":1}\u0000{\"b\":\"x\"}\u0000".toByteArray(Charsets.UTF_8),
            mutableMapOf("q" to 100, "a" to 1, "b" to "x")
        )
    }

    @Test
    fun testRunSerializedJournalDeep() {
        assertRunSerializedJournal(
            mutableMapOf("q" to 100),
            "{\"a.b.-1\":1}\u0000{\"b.-1.\":\"x\"}\u0000".toByteArray(Charsets.UTF_8),
            mutableMapOf(
                "q" to 100,
                "a" to mapOf<String, Any>("b" to listOf<Any>(1.toLong())),
                "b" to listOf<Any>(listOf<Any>("x"))
            )
        )
    }

    @Test
    fun testDeserializeFail() {
        assertDeserializeFails(
            "{\"a.\":10false}\u0000".toByteArray(Charsets.UTF_8)
        )
    }

    private fun assertDeserializeFails(serialized: ByteArray) {
        val logger = InterceptingLogger()
        assertNull(logger.msg)
        deserialize(serialized, logger)
        assertNotNull(logger.msg)
    }

    private fun assertSerializeDeserialize(serialized: ByteArray, vararg entries: Journal.Command) {
        val journal = Journal()
        for (entry in entries) {
            journal.add(entry)
        }
        val expectedBytes = serialized
        val actualBytes = serialize(journal)
        Assert.assertArrayEquals(expectedBytes, actualBytes)

        val expectedEntry = journal
        val actualEntry = deserialize(serialized, NoopLogger)
        assertEquivalent(expectedEntry, actualEntry)
    }

    private fun assertEquivalent(expected: Journal, actual: Journal) {
        val bytesExpected = serialize(expected)
        val bytesActual = serialize(actual)
        Assert.assertArrayEquals(bytesExpected, bytesActual)
    }

    fun serialize(journal: Journal): ByteArray {
        val outs = ByteArrayOutputStream()
        journal.serialize(outs)
        return outs.toByteArray()
    }

    fun deserialize(bytes: ByteArray, logger: Logger): Journal {
        return Journal.deserialize(bytes, logger)
    }

    private fun assertRunSerializedJournal(
        document: MutableMap<String, Any>,
        bytes: ByteArray,
        expectedDocument: Any
    ) {
        val journal = deserialize(bytes, NoopLogger)
        val actualDocument = journal.applyTo(document)
        val expectedMap = BugsnagTestUtils.normalized(expectedDocument)
        val actualMap = BugsnagTestUtils.normalized(actualDocument)
        Assert.assertEquals(expectedMap, actualMap)
    }
}
