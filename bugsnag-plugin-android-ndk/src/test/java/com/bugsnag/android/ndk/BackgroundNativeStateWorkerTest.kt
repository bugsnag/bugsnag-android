package com.bugsnag.android.ndk

import com.bugsnag.android.Logger
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class BackgroundNativeStateWorkerTest {

    @Test
    fun enqueuesStateUpdatesInSubmissionOrder() {
        val executor = RecordingExecutorService()
        val worker = BackgroundNativeStateWorker(object : Logger {}, executor)
        val updates = mutableListOf<String>()

        worker.enqueue { updates += "session" }
        worker.enqueue { updates += "memory" }

        assertEquals(2, executor.pendingTaskCount)
        assertEquals(emptyList<String>(), updates)

        executor.runAll()

        assertEquals(listOf("session", "memory"), updates)
    }

    private class RecordingExecutorService : AbstractExecutorService() {
        private val shutdown = AtomicBoolean(false)
        private val tasks = mutableListOf<Runnable>()

        val pendingTaskCount: Int
            get() = tasks.size

        fun runAll() {
            while (tasks.isNotEmpty()) {
                tasks.removeAt(0).run()
            }
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
