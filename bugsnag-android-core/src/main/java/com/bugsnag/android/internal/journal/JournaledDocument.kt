package com.bugsnag.android.internal.journal

import android.os.Build
import androidx.annotation.RequiresApi
import com.bugsnag.android.internal.MemoryMappedOutputStream
import com.bugsnag.android.internal.asConcurrent
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.nio.BufferOverflowException
import java.util.function.BiConsumer

/**
 * A document with journal backing.
 * Document snapshots are persisted to files, and journal entries are persisted to a memory-mapped
 * file.
 *
 * This document will always be internally composed of ConcurrentHashMap and CopyOnWriteArrayList.
 * Thus, the document and its children will have the same concurrency protections and requirements
 * of those classes.
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
 * @param highWater The "high water" point. If the size of the serialized journal exceeds this, the
 *                  journal will be snapshotted when snapshotIfHighWater() is called.
 * @param initialDocument The initial document contents (will be deep copied)
 */
constructor(
    baseDocumentPath: File,
    journalType: String,
    version: Int,
    bufferSize: Long,
    private val highWater: Long,
    initialDocument: Map<String, Any>
) : Map<String, Any>, Closeable {

    private val journalPath = getRuntimeJournalPath(baseDocumentPath)
    private val snapshotPath = getSnapshotPath(baseDocumentPath)
    private val newSnapshotPath = getNewSnapshotPath(baseDocumentPath)

    private var document = convertDocumentToConcurrent(initialDocument)
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
     *
     * @param path The path describing where to set the value
     * @param value The value to set
     * @see DocumentPath
     */
    fun addCommand(path: String, value: Any?) {
        return addCommand(Journal.Command(path, value))
    }

    /**
     * Add a journal command.
     *
     * This will first serialize the command to shared memory. If the memory is too full,
     * it will create a snapshot to free up some room and try a second time.
     * Next, it will apply the command to the document, and then add it to the journal itself.
     */
    fun addCommand(command: Journal.Command) {
        synchronized(journal) {
            requireOpen()
            internalAddCommand(command)
        }
    }

    fun addCommands(vararg commands: Journal.Command) {
        synchronized(journal) {
            requireOpen()
            commands.forEach { internalAddCommand(it) }
        }
    }

    fun addCommands(vararg commands: Pair<String, Any?>) {
        synchronized(journal) {
            requireOpen()
            commands.forEach { internalAddCommand(it.first, it.second) }
        }
    }

    private fun internalAddCommand(documentPath: String, value: Any?) {
        internalAddCommand(Journal.Command(documentPath, value))
    }

    private fun internalAddCommand(command: Journal.Command) {
        try {
            command.serialize(journalStream)
        } catch (ex: BufferOverflowException) {
            snapshot()
            command.serialize(journalStream)
        }
        document = command.apply(document)
        journal.add(command)
    }

    /**
     * Save a current document snapshot and clear the journal
     */
    fun snapshot() {
        synchronized(journal) {
            snapshotInternal()
        }
    }

    /**
     * Save a current snapshot if the journal has passed the high water mark.
     * Use this in a BG thread to reduce the incidence of snapshotting on the critical path.
     */
    fun snapshotIfHighWater() {
        if (journalStream.bufferSize - journalStream.bytesRemaining() >= highWater) {
            synchronized(journal) {
                // Second check in case someone else beat us to it
                if (journalStream.bufferSize - journalStream.bytesRemaining() >= highWater) {
                    snapshotInternal()
                }
            }
        }
    }

    /**
     * Close the journal backing this document. All further modifications will throw exceptions.
     */
    override fun close() {
        synchronized(journal) {
            if (!isOpen) {
                return
            }
            try {
                snapshotInternal()
            } finally {
                isOpen = false
                journalStream.close()
            }
        }
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

    private fun snapshotInternal() {
        requireOpen()
        JsonHelper.serialize(document, newSnapshotPath)
        journal.clear()
        journalStream.clear()
        journal.serialize(journalStream)
        if (!newSnapshotPath.renameTo(snapshotPath)) {
            throw FileSystemException(newSnapshotPath, snapshotPath, "Could not rename file")
        }
    }

    private fun requireOpen() {
        if (!isOpen) {
            throw IllegalStateException("Cannot modify a closed document")
        }
    }

    companion object {
        // 0x99 is guaranteed to be an invalid UTF-8 start byte
        const val clearedByteValue: Byte = 0x99.toByte()

        internal fun getSnapshotPath(basePath: File): File {
            return File(basePath.path + ".snapshot")
        }

        internal fun getNewSnapshotPath(basePath: File): File {
            return File(basePath.path + ".snapshot.new")
        }

        internal fun getRuntimeJournalPath(basePath: File): File {
            return File(basePath.path + ".journal")
        }

        internal fun getCrashtimeJournalPath(basePath: File): File {
            return File(basePath.path + ".journal.crashtime")
        }

        internal fun getBaseDocumentPath(runtimeJournalPath: File): File {
            return File(runtimeJournalPath.parentFile, runtimeJournalPath.nameWithoutExtension)
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
         * @return The document, or null if no journal files exist at the path.
         */
        fun loadDocumentContents(baseDocumentPath: File): MutableMap<in String, out Any>? {
            val runtimeJournalPath = getRuntimeJournalPath(baseDocumentPath)
            val crashtimeJournalPath = getCrashtimeJournalPath(baseDocumentPath)
            val snapshotPath = getSnapshotPath(baseDocumentPath)
            val newSnapshotPath = getNewSnapshotPath(baseDocumentPath)

            // If mydoc.snapshot.new exists, it means we were in the middle of writing a new
            // snapshot when the app terminated, so:
            //  * mydoc.journal doesn't contain newer information than mydoc.snapshot.new.
            //  * mydoc.journal.crashtime will contain newer info if we crashed.
            //  * mydoc.snapshot.new may be corrupt if we crashed mid-snapshot.
            try {
                return applyJournal(crashtimeJournalPath, JsonHelper.deserialize(newSnapshotPath))
            } catch (ex: IOException) {
                // mydoc.snapshot.new doesn't exist, or it's corrupt.
                // Either way, we must fall back to using mydoc.snapshot and mydoc.journal
            }

            // At this point:
            //  * If mydoc.snapshot doesn't exist, we didn't even initialize. Return null.
            //  * If mydoc.snapshot exists, it must be valid.
            //  * mydoc.journal might exist.
            //  * mydoc.journal.crashtime might exist.

            val document = try {
                JsonHelper.deserialize(snapshotPath)
            } catch (ex: IOException) {
                // If we don't even have a base snapshot, we have nothing at all.
                return null
            }

            // The journal files might exist and be valid. Apply both to the regular snapshot.
            return applyJournal(crashtimeJournalPath, applyJournal(runtimeJournalPath, document))
        }

        private fun applyJournal(journalPath: File, document: MutableMap<in String, out Any>):
            MutableMap<in String, out Any> {
            val journal = try {
                Journal.deserialize(journalPath)
            } catch (ex: Exception) {
                // The journal file is missing or corrupt, so we won't use it
                return document
            }

            return journal.applyTo(document)
        }

        internal fun convertDocumentToConcurrent(document: Map<String, Any>): MutableMap<String, Any> {
            @Suppress("UNCHECKED_CAST")
            return document.asConcurrent() as MutableMap<String, Any>
        }
    }
}
