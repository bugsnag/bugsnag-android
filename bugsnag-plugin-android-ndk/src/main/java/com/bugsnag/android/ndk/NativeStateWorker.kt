package com.bugsnag.android.ndk

import androidx.annotation.VisibleForTesting
import com.bugsnag.android.Logger
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

internal interface NativeStateWorker {
    fun enqueue(task: () -> Unit)

    fun shutdown()
}

internal class BackgroundNativeStateWorker(
    private val logger: Logger,
    @get:VisibleForTesting internal val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Bugsnag NDK state").apply {
            isDaemon = true
        }
    }
) : NativeStateWorker {

    override fun enqueue(task: () -> Unit) {
        try {
            executor.execute(task)
        } catch (exc: RejectedExecutionException) {
            logger.w("Failed to process native state update.", exc)
        }
    }

    override fun shutdown() {
        executor.shutdown()
    }
}
