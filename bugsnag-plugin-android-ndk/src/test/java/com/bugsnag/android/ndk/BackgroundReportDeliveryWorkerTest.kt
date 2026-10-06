package com.bugsnag.android.ndk

import com.bugsnag.android.Logger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class BackgroundReportDeliveryWorkerTest {

    @Test
    fun enqueueScansOnBackgroundExecutor() {
        val reportDirectory = Files.createTempDirectory("bugsnag-report-worker").toFile()
        val keepFile = File(reportDirectory, "keep.json").apply {
            writeText("{" + "\"app\":{},\"exceptions\":[]" + "}")
        }
        val discardFile = File(reportDirectory, "discard.static_data.json").apply {
            writeText("ignored")
        }

        val deliveredFiles = mutableListOf<File>()
        val executor = RecordingExecutorService()
        val worker = BackgroundReportDeliveryWorker(
            logger = object : Logger {},
            reportDirectory = reportDirectory,
            reportDiscardScannerFactory = {
                ReportDiscardScanner(object : Logger {}, emptySet())
            },
            reportDeliverer = { deliveredFiles += it },
            executor = executor
        )

        worker.enqueue()

        assertEquals(1, executor.pendingTaskCount)
        assertTrue(deliveredFiles.isEmpty())

        executor.runNext()

        assertEquals(listOf(keepFile), deliveredFiles)
        assertTrue(keepFile.exists())
        assertFalse(discardFile.exists())
    }

    private class RecordingExecutorService : AbstractExecutorService() {
        private val shutdown = AtomicBoolean(false)
        private val tasks = mutableListOf<Runnable>()

        val pendingTaskCount: Int
            get() = tasks.size

        fun runNext() {
            tasks.removeAt(0).run()
        }

        override fun shutdown() {
            shutdown.set(true)
        }

        override fun shutdownNow(): MutableList<Runnable> {
            shutdown.set(true)
            val pending = tasks.toMutableList()
            tasks.clear()
            return pending
        }

        override fun isShutdown(): Boolean = shutdown.get()

        override fun isTerminated(): Boolean = shutdown.get() && tasks.isEmpty()

        override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true

        override fun execute(command: Runnable) {
            if (shutdown.get()) {
                throw RejectedExecutionException("executor is shut down")
            }
            tasks += command
        }
    }
}
