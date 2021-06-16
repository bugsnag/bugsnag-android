package com.bugsnag.android

import com.dslplatform.json.DslJson
import java.io.IOException
import java.io.OutputStream

/**
 * A journal represents a series of commands that will manipulate a document.
 * Sometimes it's more efficient to store a base document and a list of modifications
 * to be made to it rather than re-serialize the entire document after every modification.
 */
class Journal {
    val commands: MutableList<Command> = mutableListOf()

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
    fun applyTo(document: MutableMap<String, Any>): Any {
        var updatedDocument = document
        for (command in commands) {
            updatedDocument = command.apply(updatedDocument)
        }
        return updatedDocument
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
     * Serialize the journal to a stream.
     *
     * @param out The stream to serialize to.
     * @throws IOException if an IO exception occurs.
     */
    @Throws(IOException::class)
    fun serialize(out: OutputStream) {
        for (command in commands) {
            command.serialize(out)
            out.write(0)
            out.flush()
        }
    }

    /**
     * Represents a single journal command, consisting of a document path,
     * and a value to be applied at that path level.
     */
    class Command(private val pathAsString: String, val value: Any?) {
        private val path: DocumentPath = DocumentPath(pathAsString)

        /**
         * Apply this journal command to a document.
         *
         * @param document The document to modify.
         * @return The resulting document (may not be the same as the one passed in).
         */
        fun apply(document: MutableMap<String, Any>): MutableMap<String, Any> {
            return path.modifyDocument(document, value)
        }

        /**
         * Serialize to an OutputStream
         *
         * @param out The stream
         * @throws IOException If an IO exception occurs
         */
        @Throws(IOException::class)
        fun serialize(out: OutputStream) {
            val command: MutableMap<String, Any?> = HashMap()
            command[pathAsString] = value
            dslJson.serialize(command, out)
        }
    }

    companion object {
        val dslJson = DslJson<Any>()

        /**
         * Deserialize from a byte array.
         *
         * @param bytes The bytes to deserialize from
         * @return The journal
         */
        fun deserialize(bytes: ByteArray, logger: Logger): Journal {
            val journal = Journal()
            var currentStart = 0
            for (i in bytes.indices) {
                if (bytes[i] == 0.toByte()) {
                    try {
                        val document = bytes.copyOfRange(currentStart, i)
                        journal.add(deserializeCommand(dslJson, document))
                        currentStart = i + 1
                    } catch (exception: Exception) {
                        logger.e(
                            "Could not deserialize journal entry" + bytes.contentToString(),
                            exception
                        )
                    }
                }
            }
            return journal
        }

        /**
         * Deserialize a command from a byte buffer.
         *
         * @param document The document to deserialize from
         * @return The journal command
         * @throws IOException If an IO exception occurs
         */
        @Throws(IOException::class)
        fun deserializeCommand(dslJson: DslJson<Any>, document: ByteArray): Command {
            @Suppress("UNCHECKED_CAST")
            val map: Map<String, Any>? = dslJson.deserialize(
                MutableMap::class.java,
                document,
                document.size
            ) as Map<String, Any>?
            require(!(map == null || map.isEmpty())) { "Document is invalid" }
            val mapEntry = map.entries.iterator().next()
            return Command(mapEntry.key, mapEntry.value)
        }
    }
}
