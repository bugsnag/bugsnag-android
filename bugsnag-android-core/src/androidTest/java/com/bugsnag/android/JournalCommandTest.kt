package com.bugsnag.android

import com.bugsnag.android.internal.Journal
import com.dslplatform.json.DslJson
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream

class JournalCommandTest {
    @Test
    fun testSerializeBasic() {
        assertSerializeDeserialize(
            "{\"a\":1}\u0000",
            Journal.Command("a", 1)
        )
        assertSerializeDeserialize(
            "{\"a\":1.5}\u0000",
            Journal.Command("a", 1.5)
        )
        assertSerializeDeserialize(
            "{\"a\":\"x\"}\u0000",
            Journal.Command("a", "x")
        )
        assertSerializeDeserialize(
            "{\"a\":true}\u0000",
            Journal.Command("a", true)
        )
        assertSerializeDeserialize(
            "{\"a\":null}\u0000",
            Journal.Command("a", null)
        )
    }

    private fun serializeJournalEntry(entry: Journal.Command): String {
        val os = ByteArrayOutputStream()
        entry.serialize(os)
        return os.toString(Charsets.UTF_8.name())
    }

    private fun deserializeJournalEntry(json: String): Journal.Command {
        val dslJson = DslJson<MutableMap<String, Any>>()
        val document = json.toByteArray(Charsets.UTF_8)
        return Journal.deserializeCommand(dslJson, document)
    }

    private fun assertSerializeDeserialize(json: String, entry: Journal.Command) {
        val expectedJson = json
        val observedJson = serializeJournalEntry(entry)
        Assert.assertEquals(expectedJson, observedJson)

        val expectedEntry = entry
        val observedEntry = deserializeJournalEntry(json)
        assertEquivalent(expectedEntry, observedEntry)
    }

    private fun assertEquivalent(expected: Journal.Command, observed: Journal.Command) {
        val expectedSerialized = serializeJournalEntry(expected)
        val observedSerialized = serializeJournalEntry(observed)
        Assert.assertEquals(expectedSerialized, observedSerialized)
    }
}
