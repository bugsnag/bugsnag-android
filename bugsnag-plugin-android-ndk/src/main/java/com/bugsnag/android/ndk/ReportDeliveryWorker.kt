package com.bugsnag.android.ndk

import androidx.annotation.VisibleForTesting
import com.bugsnag.android.Logger
import com.bugsnag.android.NativeInterface
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

internal interface ReportDeliveryWorker {
    fun enqueue()

    fun shutdown()
}

internal class BackgroundReportDeliveryWorker(
    private val logger: Logger,
    private val reportDirectory: File = NativeInterface.getNativeReportPath(),
    private val reportDiscardScannerFactory: () -> ReportDiscardScanner = { ReportDiscardScanner(logger) },
    private val reportDeliverer: (File) -> Unit = NativeInterface::deliverReport,
    @get:VisibleForTesting internal val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Bugsnag NDK report delivery").apply {
            isDaemon = true
        }
    }
) : ReportDeliveryWorker {

    override fun enqueue() {
        try {
            executor.execute { deliverPendingReports() }
        } catch (exc: RejectedExecutionException) {
            logger.w("Failed to process pending native reports, retaining them for later.", exc)
        }
    }

    override fun shutdown() {
        executor.shutdown()
    }

    @VisibleForTesting
    internal fun deliverPendingReports() {
        val discardScanner = reportDiscardScannerFactory()
        reportDirectory.listFiles()?.forEach { reportFile ->
            if (discardScanner.shouldDiscard(reportFile)) {
                reportFile.delete()
            } else {
                reportDeliverer(reportFile)
            }
        }
    }
}
