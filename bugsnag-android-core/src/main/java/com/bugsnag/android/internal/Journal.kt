package com.bugsnag.android.internal

import com.dslplatform.json.DslJson
import java.io.IOException
import java.io.OutputStream

/**
 * A journal represents a series of commands that will manipulate a document.
 * Sometimes it's more efficient to serialize a base document and a list of modifications
 * to be made to it rather than to re-serialize the entire document after every modification.
 */
class Journal {
    private val commands: MutableList<Command> = mutableListOf()

    /**
     * Apply this journal to a document.
     *
     * This method goes through the journal entries one-by-one, applying their modification
     * commands to the document. This is a destructive operation that will modify the document.
     *
     * Note: If a journal command replaces the root object, the resulting document will be a
     * different object from the one passed in.
     *
     * @param document The document to apply to.
     * @return The finished document (may be a different object from the one passed in).
     */
    fun applyTo(document: MutableMap<in String, out Any>): MutableMap<in String, out Any> {
        return commands.fold(document) { doc, command -> command.apply(doc) }
    }

    /**
     * Add a journal command to this journal
     *
     * @param command The journal command
     */
    fun add(command: Command) {
        commands.add(command)
    }

    /**
     * Clear the journal, removing all journal entries.
     */
    fun clear() {
        commands.clear()
    }

    /**
     * Serialize this journal to a stream.
     *
     * @param out The stream to serialize to.
     * @throws IOException if an IO exception occurs.
     */
    @Throws(IOException::class)
    fun serialize(out: OutputStream) {
        for (command in commands) {
            command.serialize(out)
        }
    }

    /**
     * Command represents a single journal command, consisting of:
     * - a document path, which determines where in a document the change is to be made.
     * - a value, which determines the change to be made.
     *
     * In general, a null value represents a delete, and a non-null value represents an insert or
     * replace.
     *
     * @see DocumentPath for a description of how path strings work.
     */
    class Command(private val path: String, val value: Any?) {
        private val documentPath: DocumentPath = DocumentPath(path)

        /**
         * Apply this journal command to a document.
         *
         * @param document The document to modify.
         * @return The resulting document (may not be the same as the one passed in).
         */
        fun apply(document: MutableMap<in String, out Any>): MutableMap<in String, out Any> {
            return documentPath.modifyDocument(document, value)
        }

        /**
         * Serialize this command to an OutputStream
         *
         * @param out The stream
         * @throws IOException If an IO exception occurs
         */
        @Throws(IOException::class)
        fun serialize(out: OutputStream) {
            dslJson.serialize(mutableMapOf(path to value), out)
            out.write(0)
        }
    }

    companion object {
        // Only one global DslJson is needed, and is thread-safe
        // Note: dsl-json adds about 150k to the final binary size.
        internal val dslJson = DslJson<MutableMap<String, Any>>()

        private const val NULL_BYTE = 0.toByte()

        /**
         * Deserialize an entire journal from a byte array.
         *
         * Note: This method will skip any journal command entries that fail to deserialize
         * (corrupt, invalid, or malformed data) and generate log entries describing the problem(s).
         *
         * @param bytes The bytes to deserialize from
         * @return The journal
         */
        fun deserialize(bytes: ByteArray): Journal {
            val journal = Journal()
            var currentStart = 0
            for (i in bytes.indices) {
                if (bytes[i] == NULL_BYTE) {
                    try {
                        val document = bytes.copyOfRange(currentStart, i)
                        journal.add(deserializeCommand(dslJson, document))
                        currentStart = i + 1
                    } catch (exception: Exception) {
                        throw IllegalStateException(
                            "Could not deserialize journal entry: ${bytes.contentToString()}: $exception"
                        )
                    }
                }
            }
            return journal
        }

        /**
         * Deserialize a journal command from a byte buffer.
         *
         * @param document The document to deserialize from
         * @return The journal command
         * @throws IOException If an IO exception occurs
         */
        @Throws(IOException::class)
        fun deserializeCommand(dslJson: DslJson<MutableMap<String, Any>>, document: ByteArray): Command {
            @Suppress("UNCHECKED_CAST")
            val map: MutableMap<String, Any>? = dslJson.deserialize(
                MutableMap::class.java,
                document,
                document.size
            ) as MutableMap<String, Any>?
            require(!map.isNullOrEmpty()) { "Journal entry data is corrupt and could not be decoded" }
            val mapEntry = map.entries.single()
            return Command(mapEntry.key, mapEntry.value)
        }
    }
}
