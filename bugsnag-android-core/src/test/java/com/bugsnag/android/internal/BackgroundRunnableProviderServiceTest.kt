package com.bugsnag.android.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.lang.Thread
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit

private const val WAIT_TIME_MS = 200L
private const val CONFINEMENT_TEST_ATTEMPTS = 20

internal class BackgroundRunnableProviderServiceTest {

    /**
     * Verifies that the task type submits a Runnable to the correct executor.
     */
    @Test
    fun testSubmitRunnable() {
        val service = BackgroundTaskService()
        runSubmitRunnableTest(service, TaskType.ERROR_REQUEST, "Bugsnag Error thread")
        runSubmitRunnableTest(service, TaskType.SESSION_REQUEST, "Bugsnag Session thread")
        runSubmitRunnableTest(service, TaskType.IO, "Bugsnag IO thread")
        runSubmitRunnableTest(service, TaskType.INTERNAL_REPORT, "Bugsnag Internal Report thread")
        runSubmitRunnableTest(service, TaskType.DEFAULT, "Bugsnag Default thread")
    }

    private fun runSubmitRunnableTest(
        service: BackgroundTaskService,
        taskType: TaskType,
        expectedName: String
    ) {
        val runnable = Runnable {
            val name = Thread.currentThread().name
            assertEquals(expectedName, name)
        }
        val future = service.submitTask(taskType, runnable)
        assertNull(future.get(WAIT_TIME_MS, TimeUnit.MILLISECONDS))
    }

    /**
     * Verifies that the task type submits a Callable to the correct executor.
     */
    @Test
    fun testSubmitCallable() {
        val service = BackgroundTaskService()
        runSubmitCallableTest(service, TaskType.ERROR_REQUEST, 1, "Bugsnag Error thread")
        runSubmitCallableTest(service, TaskType.SESSION_REQUEST, 2, "Bugsnag Session thread")
        runSubmitCallableTest(service, TaskType.IO, 3, "Bugsnag IO thread")
        runSubmitCallableTest(
            service,
            TaskType.INTERNAL_REPORT,
            4,
            "Bugsnag Internal Report thread"
        )
        runSubmitCallableTest(service, TaskType.DEFAULT, 5, "Bugsnag Default thread")
    }

    private fun runSubmitCallableTest(
        service: BackgroundTaskService,
        taskType: TaskType,
        result: Int,
        expectedName: String
    ) {
        val callable = Callable {
            val name = Thread.currentThread().name
            assertEquals(expectedName, name)
            result
        }
        val future = service.submitTask(taskType, callable)
        assertEquals(result, future.get(WAIT_TIME_MS, TimeUnit.MILLISECONDS))
    }

    /**
     * Validates that each executor only uses a single thread by submitting tasks ~20 times
     * by verifying the thread ID is always the same.
     */
    @Test
    fun testThreadConfinement() {
        val service = BackgroundTaskService()
        TaskType.values().forEach { taskType ->
            runThreadConfinementTest(service, taskType)
        }
    }

    private fun runThreadConfinementTest(
        service: BackgroundTaskService,
        taskType: TaskType
    ) {
        val threadIds = mutableSetOf<Long>()
        repeat(CONFINEMENT_TEST_ATTEMPTS) {
            val future = service.submitTask(
                taskType,
                Runnable {
                    threadIds.add(Thread.currentThread().id)
                }
            )
            future.get()
        }
        assertEquals(1, threadIds.size)
    }

    /**
     * Verifies that executors are shutdown and allow tasks to gracefully complete execution.
     */
    @Test
    fun testShutdown() {
        val service = BackgroundTaskService()
        val latch = CountDownLatch(1)

        // 0. Track which tasks complete via mutable sets
        val completedFirstTasks = mutableSetOf<TaskType>()
        val completedSecondTasks = mutableSetOf<TaskType>()

        // 1. Make all the executors do work by submitting a Runnable that blocks with a CountdownLatch.
        TaskType.values().forEach { taskType ->
            submitBlockingJob(service, latch, completedFirstTasks, taskType)
        }

        // 2. submit additional tasks which wait in the work queue
        TaskType.values().forEach { taskType ->
            submitBlockingJob(service, latch, completedSecondTasks, taskType)
        }

        // 3. Shutdown the service and release latch, allowing jobs to execute
        latch.countDown()
        service.shutdown()

        // 4. verify that all executors now reject submission of new tasks
        assertRejectedExecution(service, TaskType.ERROR_REQUEST)
        assertRejectedExecution(service, TaskType.SESSION_REQUEST)
        assertRejectedExecution(service, TaskType.INTERNAL_REPORT)
        assertRejectedExecution(service, TaskType.DEFAULT)
        assertRejectedExecution(service, TaskType.IO)
    }

    /**
     * Test that tasks submitted to the same queue within a task work without deadlocking the queue.
     * This has a 5 second timeout since the symptom of a failure is thread-starvation / deadlock.
     */
    @Test(timeout = 5_000)
    fun testSubmitOnSubmit() {
        val service = BackgroundTaskService()

        TaskType.values().forEach { taskType ->
            val result = service.submitTask<Pair<TaskType, String>>(taskType) {
                service.submitTask<Pair<TaskType, String>>(taskType) {
                    taskType to "done"
                }.get()
            }.get()

            assertEquals(taskType to "done", result)
        }

        service.shutdown()
    }

    private fun submitBlockingJob(
        service: BackgroundTaskService,
        latch: CountDownLatch,
        completedTasks: MutableSet<TaskType>,
        taskType: TaskType
    ) {
        service.submitTask(
            taskType,
            Runnable {
                latch.await()
                Thread.sleep(10)
                completedTasks.add(taskType)
            }
        )
    }

    private fun assertRejectedExecution(
        service: BackgroundTaskService,
        taskType: TaskType
    ) {
        try {
            service.submitTask(taskType, Runnable { })
            throw IllegalStateException()
        } catch (ignored: RejectedExecutionException) {
        }
    }
}
