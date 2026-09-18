package com.bugsnag.android.ndk

import com.bugsnag.android.Logger
import com.bugsnag.android.StateEvent
import com.bugsnag.android.internal.BackgroundTaskService
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal class NativeBridgeTest {

    @Test
    fun deliverPendingDelegatesToBackgroundWorker() {
        val worker = RecordingReportDeliveryWorker()
        val bridge = NativeBridge(
            BackgroundTaskService(),
            object : Logger {},
            { worker }
        )

        forceInstalled(bridge)

        bridge.onStateChange(StateEvent.DeliverPending)
        bridge.shutdown()

        assertEquals(1, worker.enqueueCalls.get())
        assertEquals(1, worker.shutdownCalls.get())
    }

    @Test
    fun addBreadcrumbDelegatesToBackgroundExecutor() {
        val defaultExecutor = RecordingExecutorService()
        val backgroundService = BackgroundTaskService(
            errorExecutor = defaultExecutor,
            sessionExecutor = defaultExecutor,
            ioExecutor = defaultExecutor,
            internalReportExecutor = defaultExecutor,
            defaultExecutor = defaultExecutor
        )
        val bridge = NativeBridge(
            backgroundService,
            object : Logger {},
            { RecordingReportDeliveryWorker() }
        )

        forceInstalled(bridge)

        bridge.onStateChange(
            StateEvent.AddBreadcrumb(
                message = "breadcrumb",
                type = com.bugsnag.android.BreadcrumbType.LOG,
                timestamp = "2026-09-18T00:00:00Z",
                metadata = mutableMapOf("key" to "value")
            )
        )

        assertEquals(1, defaultExecutor.pendingTaskCount)
    }

    private fun forceInstalled(bridge: NativeBridge) {
        val field = NativeBridge::class.java.getDeclaredField("installed")
        field.isAccessible = true
        val installed = field.get(bridge) as java.util.concurrent.atomic.AtomicBoolean
        installed.set(true)
    }

    private class RecordingExecutorService : AbstractExecutorService() {
        private val shutdown = java.util.concurrent.atomic.AtomicBoolean(false)
        private val tasks = mutableListOf<Runnable>()

        val pendingTaskCount: Int
            get() = tasks.size

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

    private class RecordingReportDeliveryWorker : ReportDeliveryWorker {
        val enqueueCalls = AtomicInteger(0)
        val shutdownCalls = AtomicInteger(0)

        override fun enqueue() {
            enqueueCalls.incrementAndGet()
        }

        override fun shutdown() {
            shutdownCalls.incrementAndGet()
        }
    }
}
