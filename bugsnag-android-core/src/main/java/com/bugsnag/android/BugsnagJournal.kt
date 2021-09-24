package com.bugsnag.android

import com.bugsnag.android.internal.journal.JournalKeys
import com.bugsnag.android.internal.journal.JournaledDocument
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.lang.Thread
import kotlin.concurrent.thread

/**
 * The main journal contains the document that will be sent to the back end whenever an event occurs.
 * All changes to the document must be made via journal commands to ensure that they are preserved
 * in the event of a crash.
 *
 * The single instance to be used in Bugsnag is "instance", and startInstance() must be called before
 * using it.
 */
internal class BugsnagJournal @JvmOverloads internal constructor (
    private val logger: Logger,
    baseDocumentPath: File,
    initialDocument: Map<String, Any> = mapOf()
) : Closeable {
    internal val journal = JournaledDocument(
        baseDocumentPath,
        journalType,
        journalVersion,
        mmapBufferSize,
        highWater,
        withInitialDocumentContents(initialDocument)
    )

    init {
        beginSnapshotHousekeepingThread()
    }

    /**
     * The document in its current state (with all current journal entries applied).
     * Do NOT modify any of the document's contents directly; information will be lost if you do.
     * Use addCommand() instead to modify the document.
     */
    val document: Map<String, Any> get() = journal

    private var housekeepingThreadShouldKeepRunning = false

    /**
     * Add a journal command.
     *
     * @param documentPath The path describing where in the document to set the value
     * @param value The value to set
     * @see DocumentPath
     */
    fun addCommand(documentPath: String, value: Any?) {
        if (documentPath.isEmpty()) {
            logger.e("addCommand called with empty path (replace entire document not allowed)")
            return
        }
        try {
            return journal.addCommand(documentPath, value)
        } catch (exc: IOException) {
            logger.e("Could not add journal command", exc)
        }
    }

    /**
     * Add multiple journal commands at once. Since adding commands locks a mutex, this can give
     * substantial savings.
     *
     * @param commands: Pairs of path/value to be added as journal commands.
     * @see DocumentPath
     */
    fun addCommands(vararg commands: Pair<String, Any?>) {
        if (commands.any { (path, _) -> path.isEmpty() }) {
            logger.e("addCommands called with empty path (replace entire document not allowed)")
            return
        }

        try {
            return journal.addCommands(*commands)
        } catch (exc: IOException) {
            logger.e("Could not add journal commands", exc)
        }
    }

    /**
     * Save a current document snapshot and clear the journal.
     * You'll rarely need to call this since the housekeeping thread already snapshots periodically.
     */
    fun snapshot() {
        try {
            journal.snapshot()
        } catch (exc: IOException) {
            logger.e("Could not write journal snapshot", exc)
        }
    }

    private fun beginSnapshotHousekeepingThread() {
        housekeepingThreadShouldKeepRunning = true
        thread {
            while (housekeepingThreadShouldKeepRunning) {
                try {
                    journal.snapshotIfHighWater()
                } catch (exc: IOException) {
                    logger.e("Could not write journal snapshot; exiting housekeeping thread...", exc)
                    break
                }
                Thread.sleep(highWaterPollingIntervalMS)
            }
        }
    }

    override fun close() {
        housekeepingThreadShouldKeepRunning = false
    }

    companion object {
        // The "type" of the main journal. This will probably never change.
        internal const val journalType = "Bugsnag Android"

        // Version of this journal. This will change for migration purposes.
        internal const val journalVersion = 1

        // Size of the on-disk memory-mapped buffer in bytes.
        private const val mmapBufferSize = 100000L

        // If the memory-mapped buffer fills beyond this many bytes, it gets auto-snapshotted.
        // Note: The size of mmapBufferSize and highWater have no effect on snapshot runtime cost.
        private const val highWater = 50000L

        // Polling interval in milliseconds for the high water check thread.
        private const val highWaterPollingIntervalMS = 200L

        /**
         * Load the previous document at this path
         * @param baseDocumentPath The base path to load the journal data from
         * @return a map containing the document, or null if no journal exists at this path.
         */
        @JvmStatic
        fun loadPreviousDocument(baseDocumentPath: File): MutableMap<in String, out Any>? {
            return JournaledDocument.loadDocumentContents(baseDocumentPath)
        }

        // Internal to keep it accessible to unit tests
        internal fun withInitialDocumentContents(document: Map<String, Any>): Map<String, Any> {
            val docWithVersionInfo = document.toMutableMap()
            docWithVersionInfo[JournalKeys.keyVersionInfo] = mapOf(
                JournalKeys.keyType to journalType,
                JournalKeys.keyVersion to journalVersion
            )
            return docWithVersionInfo
        }
    }
}
