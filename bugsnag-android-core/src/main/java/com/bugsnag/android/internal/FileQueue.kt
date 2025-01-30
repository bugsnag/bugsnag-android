package com.bugsnag.android.internal

import com.bugsnag.android.JsonStream
import com.bugsnag.android.JsonStream.Streamable
import com.bugsnag.android.Logger
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.Writer
import java.util.Collections
import java.util.TreeSet
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

typealias WriteErrorHandler = (exception: Exception?, errorFile: File?, context: String?) -> Unit

internal class FileQueue(
    val storageDir: File,
    private val maxStoreCount: Int,
    private val logger: Logger,
    private val comparator: Comparator<in File>,
    private val writeErrorHandler: WriteErrorHandler?,
    /**
     * Callback invoked whenever the queue becomes empty (all files are blocked or deleted). The
     * callback is always invoked under a lock, ensuring that the queue will not change state
     * while the callback is being invoked.
     */
    private val onQueueEmptyCallback: () -> Unit,
) {
    /**
     * This is all of files that are (for whatever reason) not considered to be "in" the queue.
     * These can be files that (for whatever reason) cannot be deleted from the filesystem, or
     * files that are actively being written or read.
     */
    private val blockedFiles = TreeSet(comparator)

    private val lock = lockFor(storageDir)

    init {
        ensureStorageDirValid()
    }

    fun isEmpty(): Boolean = lock.withLock { isEmptyUnderLock() }

    private fun ensureStorageDirValid(): Boolean {
        try {
            storageDir.mkdirs()
        } catch (exception: Exception) {
            logger.e("Could not prepare file storage directory", exception)
            return false
        }
        return true
    }

    fun write(filename: String, streamable: Streamable): File? = lock.withLock {
        write(filename) { out ->
            val json = JsonStream(out)
            json.value(streamable)
            json.closeQuietly()
        }
    }

    inline fun write(filename: String, writeContent: (Writer) -> Unit): File? = lock.withLock {
        if (!ensureStorageDirValid()) {
            return@withLock null
        }
        if (maxStoreCount == 0) {
            return@withLock null
        }

        ensureSpaceForNewFile()
        val file = File(storageDir, filename)
        try {
            logger.d("Saving payload: $filename")
            FileOutputStream(file).bufferedWriter().use { out ->
                writeContent(out)
            }

            logger.i("Saved payload to disk: '$filename'")
            return@withLock file
        } catch (exc: FileNotFoundException) {
            logger.w("Ignoring FileNotFoundException - unable to create file", exc)
        } catch (exc: Exception) {
            writeErrorHandler?.invoke(exc, file, "Crash report serialization")
            delete(file)
        }

        return@withLock null
    }

    /**
     * Discard / delete the oldest files in the queue (according to [comparator]) until there
     * are `maxStoreCount - 1` files enqueued (allowing space for a new file to be added).
     */
    private fun ensureSpaceForNewFile() = lock.withLock {
        if (!ensureStorageDirValid()) {
            return
        }

        val fileList = enqueuedFiles()
        val discardCount = fileList.size - (maxStoreCount - 1)

        if (discardCount > 0) {
            logger.d("Discarding $discardCount old files from $storageDir")
            delete(fileList.subList(0, discardCount))
        }
    }

    /**
     * Process the next file in the queue (optionally filtered by [filter]), returning `true`
     * if a file was processed (even if the processing failed) and `false` if the queue
     * is empty or no file matched [filter].
     *
     * This function operates under a lock and guarantees that the file will not be processed
     * more than once, even if the processing fails. Upon returning any file processed by this
     * function *will be removed from the queue*.
     */
    inline fun processNextFile(
        filter: (File) -> Boolean = { true },
        processor: (File) -> Unit
    ): Boolean {
        return processFile(comparator, filter, processor)
    }

    /**
     * Same as [processNextFile] but will process the *last* (newest) file in the queue rather
     * than the "next" (allowing the queue to be treated like a stack).
     */
    inline fun processLastFile(
        filter: (File) -> Boolean = { true },
        processor: (File) -> Unit
    ): Boolean {
        return processFile(Collections.reverseOrder(comparator), filter, processor)
    }

    inline fun processEnqueuedFiles(processor: FileQueue.(List<File>) -> Unit): Int =
        lock.withLock {
            try {
                val files = enqueuedFiles()
                if (files.isEmpty()) {
                    return@withLock 0
                }

                this.processor(files)

                return@withLock files.size
            } finally {
                checkQueueEmptyUnderLock()
            }
        }

    internal inline fun processFile(
        comparator: Comparator<in File>,
        filter: (File) -> Boolean,
        processor: (File) -> Unit,
    ): Boolean = lock.withLock {
        try {
            val nextFile = getFileUnderLock(comparator, filter)
                ?: return@withLock false

            try {
                processor(nextFile)
            } catch (ex: Exception) {
                // ensure we don't attempt to process this file again
                cancelFile(nextFile)

                logger.d("failed to process file '$nextFile', will try again later", ex)
            }

            return@withLock true
        } finally {
            checkQueueEmptyUnderLock()
        }
    }

    internal inline fun getFileUnderLock(
        comparator: Comparator<in File>,
        filter: (File) -> Boolean,
    ): File? {
        val files = storageDir.listFiles()?.takeIf { it.isNotEmpty() } ?: return null
        var file: File? = null

        for (f in files) {
            if (f !in blockedFiles && filter(f)) {
                if (file == null || comparator.compare(f, file) < 0) {
                    file = f
                }
            }
        }

        return file
    }

    fun enqueuedFiles(): List<File> = lock.withLock {
        val files = storageDir.listFiles()?.toMutableList() ?: return@withLock emptyList()
        files.removeAll(blockedFiles)
        files.sortWith(comparator)

        return@withLock files
    }

    fun delete(files: Iterable<File>) = lock.withLock {
        files.forEach { file ->
            if (!file.delete()) {
                blockedFiles.add(file)
            }
        }

        checkQueueEmptyUnderLock()
    }

    fun delete(file: File) {
        if (file.parentFile != storageDir) {
            return
        }

        lock.withLock {
            if (!file.delete()) {
                blockedFiles.add(file)
            }

            checkQueueEmptyUnderLock()
        }
    }

    /**
     * Treat the given file as deleted (non-persistently). This can be used to avoid attempting
     * to re-read a file for as long as the process remains active, working around files that
     * cannot be deleted or delivered (stopping these from blocking the queue).
     */
    fun cancelFile(file: File) {
        if (file.parentFile != storageDir) {
            return
        }

        lock.withLock {
            blockedFiles.add(file)
            checkQueueEmptyUnderLock()
        }
    }

    private fun isEmptyUnderLock(): Boolean {
        val files = storageDir.listFiles() ?: return true
        if (files.isEmpty()) {
            return true
        }

        // if all of the files are blocked, the queue is empty
        return files.all { it in blockedFiles }
    }

    private fun checkQueueEmptyUnderLock() {
        if (isEmptyUnderLock()) {
            onQueueEmptyCallback()
        }
    }

    private fun Closeable.closeQuietly() {
        try {
            close()
        } catch (e: Exception) {
            // ignore
        }
    }

    internal companion object {
        /**
         * FileQueues share locks based on the directory the files are stored in. This allows
         * multiple `FileQueue` instances to be created for the same directory, without the queues
         * interfering with each other.
         */
        private val locks = HashMap<String, Lock>(4)

        /**
         * Get the shared `Lock` for the given queue directory.
         */
        @Synchronized
        fun lockFor(queueDir: File): Lock {
            return locks.getOrPut(queueDir.absolutePath) { ReentrantLock() }
        }
    }
}
