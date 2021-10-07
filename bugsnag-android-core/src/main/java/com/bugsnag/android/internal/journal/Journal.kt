package com.bugsnag.android.internal.journal

import java.io.File
import java.io.IOException
import java.io.OutputStream

/**
 * A journal represents a series of commands that will manipulate a document.
 * Sometimes it's more efficient to serialize a base document and a list of modifications
 * to be made to it rather than to re-serialize the entire document after every modification.
 */
class Journal(val type: String, val version: Int) {
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
        val journalInfoEntry = mutableMapOf<String, Any>(
            JOURNAL_INFO_PATH to mutableMapOf(
                TYPE_KEY to type,
                VERSION_KEY to version
            )
        )
        JsonHelper.serialize(journalInfoEntry, out)
        out.write(0)

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
        fun apply(document: MutableMap<in String, out Any>): MutableMap<String, Any> {
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
            JsonHelper.serialize(mutableMapOf(path to value), out)
            out.write(0)
        }

        override fun toString(): String {
            return "Command(path='$path', value=$value, documentPath=$documentPath)"
        }
    }

    companion object {
        private const val NULL_BYTE = 0.toByte()
        private const val TYPE_KEY = "type"
        private const val VERSION_KEY = "version"
        private const val JOURNAL_INFO_PATH = "*"

        /**
         * Deserialize an entire journal from a file.
         *
         * @param file The file to read from
         * @return The journal
         */
        fun deserialize(file: File): Journal {
            return deserialize(file.readBytes())
        }

        /**
         * Deserialize an entire journal from a byte array.
         *
         * @param bytes The bytes to deserialize from
         * @return The journal
         */
        fun deserialize(bytes: ByteArray): Journal {
            var currentStart = 0
            val length = bytes.size

            // Decode the journal info entry
            for (i in 0 until length) {
                if (bytes[i] == 0.toByte()) {
                    currentStart = i + 1
                    break
                }
            }
            require(currentStart > 0) { "Serialized journal doesn't contain a journal info entry" }
            val infoDocument = bytes.copyOfRange(0, currentStart - 1)
            val infoEntry = deserializeEntry(infoDocument)
            require(infoEntry.key == JOURNAL_INFO_PATH) { "Invalid or corrupt journal journal info entry" }
            val journalInfo = infoEntry.value
            require(journalInfo is MutableMap<*, *>) { "Invalid or corrupt journal journal info entry" }
            val type = journalInfo[TYPE_KEY]
            require(type != null && type is String) { "Type must exist in the journal info and be a string" }
            var version = journalInfo[VERSION_KEY]
            when (version) {
                is Int -> {}
                is Long -> {
                    version = version.toInt()
                }
                else -> {
                    throw IllegalArgumentException("Version must exist in the journal info and be an integer")
                }
            }

            val journal = Journal(type, version)

            // Decode the journal entries
            for (i in currentStart until length) {
                if (bytes[i] == NULL_BYTE) {
                    try {
                        val document = bytes.copyOfRange(currentStart, i)
                        journal.add(deserializeCommand(document))
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
        fun deserializeCommand(document: ByteArray): Command {
            val mapEntry = deserializeEntry(document)
            return Command(mapEntry.key, mapEntry.value)
        }

        /**
         * Deserialize a journal entry from a byte buffer.
         *
         * @param document The document to deserialize from
         * @return The journal entry
         * @throws IOException If an IO exception occurs
         */
        @Throws(IOException::class)
        fun deserializeEntry(document: ByteArray): Map.Entry<String, Any> {
            return JsonHelper.deserialize(document).entries.single()
        }
    }
}
