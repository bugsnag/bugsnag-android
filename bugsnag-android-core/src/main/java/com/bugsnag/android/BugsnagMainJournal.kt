package com.bugsnag.android

import com.bugsnag.android.internal.DocumentPath
import com.bugsnag.android.internal.JournaledDocument
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException
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
internal class BugsnagMainJournal internal constructor(
    private val logger: Logger,
    baseDocumentPath: File,
    initialDocument: Map<String, Any>
) {
    internal val journal = JournaledDocument(
        baseDocumentPath,
        journalType,
        journalVersion,
        mmapBufferSize,
        highWater,
        initialDocument
    )

    /**
     * The document in its current state (with all current journal entries applied).
     * Do NOT modify any of the document's contents directly; information will be lost if you do.
     * Use addCommand() instead to modify the document.
     */
    val document: Map<String, Any> get() = journal

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
        thread {
            while (true) {
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

    companion object {
        // Top-level key where version info will be saved.
        internal const val versionInfoKey = "version-info"
        internal const val journalTypeKey = "type"
        internal const val journalVersionKey = "version"

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
         * The global instance of the main journal.
         * You must call startInstance() before accessing this.
         */
        val instance: BugsnagMainJournal
            get() = if (hasInitialized) {
                mainJournal
            } else {
                throw IllegalStateException("Must call startInstance() before accessing instance")
            }

        /**
         * Start the main journal at the specified base path.
         * Any existing journal at this location will be overwritten.
         *
         * NOTE: This must be called once (and only once) before accessing instance.
         *
         * @param logger A logger
         * @param baseDocumentPath Path to a filename that will be used as the basis for all journal and snapshot files
         * @param initialDocument The initial contents of the document. This will be deep copied.
         * @return The contents of the previous journal (if any)
         */
        fun startInstance(
            logger: Logger,
            baseDocumentPath: File,
            initialDocument: Map<String, Any>
        ): MutableMap<in String, out Any>? {
            synchronized(this) {
                if (hasInitialized) {
                    return null
                }

                val previousContents = try {
                    loadPrevious(baseDocumentPath)
                } catch (exc: IOException) {
                    null
                }

                val journal = BugsnagMainJournal(logger, baseDocumentPath, initialDocument)
                mainJournal = journal
                hasInitialized = true
                journal.beginSnapshotHousekeepingThread()
                return previousContents
            }
        }

        internal var hasInitialized = false
        internal lateinit var mainJournal: BugsnagMainJournal

        // Internal to keep it accessible to unit tests
        internal fun loadPrevious(baseDocumentPath: File): MutableMap<in String, out Any> {
            return JournaledDocument.loadDocumentContents(baseDocumentPath)
        }

        // Internal to keep it accessible to unit tests
        internal fun initialDocumentContents(document: Map<String, Any>): Map<String, Any> {
            val docWithVersionInfo = document.toMutableMap()
            docWithVersionInfo[versionInfoKey] = mapOf(
                journalTypeKey to journalType,
                journalVersionKey to journalVersion
            )
            return docWithVersionInfo
        }
    }
}
