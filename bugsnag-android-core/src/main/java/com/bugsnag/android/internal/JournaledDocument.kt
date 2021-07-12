package com.bugsnag.android.internal

import android.os.Build
import androidx.annotation.RequiresApi
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException
import java.nio.BufferOverflowException
import java.util.function.BiConsumer

/**
 * A document with journal backing.
 * Document snapshots are persisted to files, and journal entries are persisted to a memory-mapped
 * file.
 *
 * Changes to the document must me made only via journal commands. Do not modify the document or any
 * of its sub-components directly!
 */
class JournaledDocument
/**
 * Constructor.
 * Upon construction, this document will have associated snapshot and journal files.
 *
 * @param baseDocumentPath The base path + filename to derive snapshot and journal names from.
 * @param journalType A string identifying the type of journal (used for verification when the
 *                    journal is reloaded)
 * @param version The journal type version (to support migration)
 * @param bufferSize The size of the shared memory buffer (and thus the maximum size of the journal)
 * @param initialDocument The initial document contents (will be shallow copied)
 */
constructor(
    baseDocumentPath: File,
    journalType: String,
    version: Int,
    bufferSize: Long,
    initialDocument: MutableMap<String, Any>
) : Map<String, Any>, Closeable {
    private val journalPath = getJournalPath(baseDocumentPath)
    private val snapshotPath = getSnapshotPath(baseDocumentPath)
    private val newSnapshotPath = getNewSnapshotPath(baseDocumentPath)

    private val document = initialDocument.toMutableMap()
    private val journal = Journal(journalType, version)
    private val journalStream = MemoryMappedOutputStream(journalPath, bufferSize, clearedByteValue)
    private var isOpen = true

    init {
        // Make sure any leftover files from the last instance at this path are gone.
        newSnapshotPath.delete()
        snapshot()
    }

    /**
     * Add a journal command.
     *
     * This will first serialize the command to shared memory. If the memory is too full,
     * it will create a snapshot to free up some room and try a second time.
     * Next, it will apply the command to the document, and then add it to the journal itself.
     */
    fun addCommand(command: Journal.Command) {
        if (!isOpen) {
            throw IllegalStateException("Cannot add commands to a closed document")
        }
        try {
            command.serialize(journalStream)
        } catch (ex: BufferOverflowException) {
            snapshot()
            command.serialize(journalStream)
        }
        command.apply(document)
        journal.add(command)
    }

    /**
     * Save a current document snapshot and clear the journal
     */
    fun snapshot() {
        if (!isOpen) {
            throw IllegalStateException("Cannot snapshot a closed document")
        }
        JsonHelper.serialize(document, newSnapshotPath)
        journal.clear()
        journalStream.clear()
        journal.serialize(journalStream)
        newSnapshotPath.renameTo(snapshotPath)
    }

    /**
     * Close the journal backing this document. All further modifications will throw exceptions.
     */
    override fun close() {
        journalStream.close()
        isOpen = false
    }

    override val entries: Set<Map.Entry<String, Any>> get() = document.entries
    override val keys: Set<String> get() = document.keys
    override val size: Int get() = document.size
    override val values: Collection<Any> get() = document.values
    override fun containsKey(key: String): Boolean { return document.containsKey(key) }
    override fun containsValue(value: Any): Boolean { return document.containsValue(value) }
    @RequiresApi(Build.VERSION_CODES.N)
    override fun forEach(action: BiConsumer<in String, in Any>) { document.forEach(action) }
    override fun get(key: String): Any? { return document[key] }
    @RequiresApi(Build.VERSION_CODES.N)
    override fun getOrDefault(key: String, defaultValue: Any): Any { return document.getOrDefault(key, defaultValue) }
    override fun isEmpty(): Boolean { return document.isEmpty() }

    companion object {
        // 0x99 is guaranteed to be an invalid UTF-8 start byte
        const val clearedByteValue: Byte = 0x99.toByte()

        internal fun getSnapshotPath(basePath: File): File {
            return File(basePath.path + ".snapshot")
        }

        internal fun getNewSnapshotPath(basePath: File): File {
            return File(basePath.path + ".snapshot.new")
        }

        internal fun getJournalPath(basePath: File): File {
            return File(basePath.path + ".journal")
        }

        /**
         * Check if a journal-backed document exists at this base path.
         *
         * @param baseDocumentPath The base path + filename to derive snapshot and journal names from.
         * @return true if document files exist at this base path.
         */
        fun documentExists(baseDocumentPath: File): Boolean {
            return getNewSnapshotPath(baseDocumentPath).exists() || getSnapshotPath(baseDocumentPath).exists()
        }

        /**
         * Return the equivalent MutableMap<String, Any> that matches the document on disk with the
         * journal on disk applied.
         *
         * Because of the many states the files can be in after a crash, this function checks the
         * files in a very specific way:
         * - Check for a ".snapshot.new" file and just return that if it's valid.
         * - Otherwise load the ".snapshot" file, which must be valid.
         *   - Attempt to load the ".journal" file, and apply it if it's valid.
         *   - Return the finished document.
         *
         * @param baseDocumentPath The base path + filename to derive snapshot and journal names from.
         * @return The document.
         */
        fun loadDocumentContents(baseDocumentPath: File): MutableMap<in String, out Any> {
            val journalPath = getJournalPath(baseDocumentPath)
            val snapshotPath = getSnapshotPath(baseDocumentPath)
            val newSnapshotPath = getNewSnapshotPath(baseDocumentPath)

            // The "new" snapshot might exist but also might be invalid.
            try {
                return JsonHelper.deserialize(newSnapshotPath)
            } catch (ex: IOException) {
                // Ignored - there is no new snapshot or it's invalid
            }

            // The base snapshot must exist and must be valid.
            val document = JsonHelper.deserialize(snapshotPath)

            // The journal might not be valid.
            val journal: Journal
            try {
                journal = Journal.deserialize(journalPath)
            } catch (ex: IOException) {
                // The journal is corrupt; just return the document.
                return document
            }

            // A valid journal must run without error.
            return journal.applyTo(document)
        }
    }
}
