package com.bugsnag.android.internal.journal

import androidx.annotation.VisibleForTesting
import com.bugsnag.android.BugsnagJournal
import com.bugsnag.android.BugsnagJournalEventMapper
import com.bugsnag.android.EventInternal
import com.bugsnag.android.Logger
import java.io.File
import java.io.IOException

internal class BugsnagJournalStore(
    private val storageDir: File,
    private val logger: Logger,
    timeProvider: () -> Long = { System.currentTimeMillis() }
) {

    internal val currentBasePath = File(storageDir, "journal_${timeProvider()}")
    private val eventMapper = BugsnagJournalEventMapper(logger)

    /**
     * Creates a new journal which will be used by the current process to record entries.
     */
    fun createNewJournal(): BugsnagJournal {
        logger.d("Creating new bugsnag journal at ${currentBasePath.path}")
        return BugsnagJournal(logger, currentBasePath)
    }

    /**
     * Processes any previous journals which are stored on disk, excluding the one that is used
     * by the current process. The [action] parameter will be invoked if a File can be converted
     * into an [EventInternal]. A file will always be deleted immediately after it has been
     * processed.
     */
    fun processPreviousJournals(action: (EventInternal) -> Unit) {
        findOldJournalFiles().forEach { file ->
            logger.d("Processing journal file ${file.path}")
            processJournalFile(file, action)
        }

        // remove any leftover files
        findAnyJournalFiles().forEach(this::deleteFile)
    }

    /**
     * Processes the most recent journal which is stored on disk, excluding the one that is used
     * by the current process. The [action] parameter will be invoked if a File can be converted
     * into an [EventInternal]. A file will always be deleted immediately after it has been
     * processed.
     */
    fun processMostRecentJournal(action: (EventInternal) -> Unit) {
        findOldJournalFiles().firstOrNull()?.let { file ->
            logger.d("Processing most recent journal file ${file.path}")
            processJournalFile(file, action)
        }
    }

    private fun processJournalFile(file: File, action: (EventInternal) -> Unit) {
        val baseDocumentPath = JournaledDocument.getBaseDocumentPath(file)
        val event = eventMapper.convertToEvent(baseDocumentPath)

        if (event != null) {
            logger.d("Converted journal file to event")
            action(event)
        } else {
            logger.d("Journal did not contain a valid event")
        }
        deleteFile(file)
    }

    /**
     * Finds .journal files from previous processes which are stored on disk.
     */
    @VisibleForTesting
    internal fun findOldJournalFiles() = findAnyJournalFiles().filter {
        it.extension == "journal"
    }

    /**
     * Finds any files related to journalling from previous processes which are stored on disk.
     * This does not filter based on file extension.
     */
    @VisibleForTesting
    internal fun findAnyJournalFiles(): List<File> {
        val files = storageDir.listFiles() ?: emptyArray()
        return files.filter { file: File ->
            !file.name.startsWith(currentBasePath.name)
        }.sortedByDescending { it.name }
    }

    private fun deleteFile(file: File) {
        try {
            if (!file.delete()) {
                file.deleteOnExit()
            }
        } catch (exc: IOException) {
            logger.w("Failed to delete old journal file", exc)
        }
    }
}
