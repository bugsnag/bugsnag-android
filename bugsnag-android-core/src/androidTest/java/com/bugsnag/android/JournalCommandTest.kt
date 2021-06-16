package com.bugsnag.android

import com.dslplatform.json.DslJson
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream

class JournalCommandTest {
    @Test
    fun testSerializeBasic() {
        assertSerializeDeserialize(
            "{\"a\":1}",
            Journal.Command("a", 1)
        )
        assertSerializeDeserialize(
            "{\"a\":1.5}",
            Journal.Command("a", 1.5)
        )
        assertSerializeDeserialize(
            "{\"a\":\"x\"}",
            Journal.Command("a", "x")
        )
        assertSerializeDeserialize(
            "{\"a\":true}",
            Journal.Command("a", true)
        )
        assertSerializeDeserialize(
            "{\"a\":null}",
            Journal.Command("a", null)
        )
    }

    private fun serializeJournalEntry(entry: Journal.Command): String {
        val os = ByteArrayOutputStream()
        entry.serialize(os)
        return os.toString(Charsets.UTF_8.name())
    }

    private fun deserializeJournalEntry(json: String): Journal.Command {
        val dslJson = DslJson<Any>()
        val document = json.toByteArray(Charsets.UTF_8)
        return Journal.deserializeCommand(dslJson, document)
    }

    private fun assertSerializeDeserialize(json: String, entry: Journal.Command) {
        val expectedJson = json
        val actualJson = serializeJournalEntry(entry)
        Assert.assertEquals(expectedJson, actualJson)

        val expectedEntry = entry
        val actualEntry = deserializeJournalEntry(json)
        assertEquivalent(expectedEntry, actualEntry)
    }

    private fun assertEquivalent(expected: Journal.Command, actual: Journal.Command) {
        val strExpected = serializeJournalEntry(expected)
        val strActual = serializeJournalEntry(actual)
        Assert.assertEquals(strExpected, strActual)
    }
}
