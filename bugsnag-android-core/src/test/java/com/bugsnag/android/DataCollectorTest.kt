package com.bugsnag.android

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.TaskType
import com.bugsnag.android.internal.dag.RunnableProvider
import com.bugsnag.android.internal.dag.ValueProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@RunWith(MockitoJUnitRunner.Silent::class)
internal class DataCollectorTest {

    private lateinit var collector: DeviceDataCollector

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var logger: Logger

    @Before
    fun setUp() {
        val res = Mockito.mock(Resources::class.java)
        `when`(res.configuration).thenReturn(Configuration())

        collector = DeviceDataCollector(
            Mockito.mock(Connectivity::class.java),
            Mockito.mock(Context::class.java),
            res,
            ValueProvider(DeviceIdStore.DeviceIds("fakeDevice", "internalFakeDevice")),
            Mockito.mock(DeviceBuildInfo::class.java),
            File("/tmp/javatest"),
            ValueProvider(false),
            Mockito.mock(BackgroundTaskService::class.java),
            Mockito.mock(Logger::class.java)
        )
    }

    @Test
    fun testCalculateFreeMemoryWithException() {
        `when`(collector.calculateFreeMemory()).thenThrow(RuntimeException())
        collector.generateDeviceWithState(0)
        Assert.assertNull(collector.calculateFreeMemory())
    }

    @Test
    fun testConcurrentAccess() {
        repeat(10) { index ->
            collector.addRuntimeVersionInfo("key" + index, "value" + index)
        }

        val count = 500

        thread {
            repeat(count) { index ->
                collector.addRuntimeVersionInfo("key" + index, "value" + index)
            }
        }

        repeat(count) {
            collector.generateDevice()
        }
    }

    @Test
    fun pendingRootDetectionIsReportedAsUnknown() {
        val res = Mockito.mock(Resources::class.java)
        `when`(res.configuration).thenReturn(Configuration())
        val pendingRootDetection = object : RunnableProvider<Boolean>() {
            override fun invoke() = false
        }
        val pendingCollector = DeviceDataCollector(
            Mockito.mock(Connectivity::class.java),
            Mockito.mock(Context::class.java),
            res,
            ValueProvider(DeviceIdStore.DeviceIds("fakeDevice", "internalFakeDevice")),
            Mockito.mock(DeviceBuildInfo::class.java),
            File("/tmp/javatest"),
            pendingRootDetection,
            Mockito.mock(BackgroundTaskService::class.java),
            Mockito.mock(Logger::class.java)
        )

        Assert.assertNull(pendingCollector.generateDevice().jailbroken)
    }

    /**
     * Simulates the IO executor being blocked (e.g. by a stalled network request) and verifies
     * that calculateFreeDisk returns within its timeout rather than blocking indefinitely.
     */
    @Test
    fun calculateFreeDiskDoesNotBlockWhenIoExecutorIsStalled() {
        val res = Mockito.mock(Resources::class.java)
        `when`(res.configuration).thenReturn(Configuration())
        val bgTaskService = BackgroundTaskService()
        val release = CountDownLatch(1)
        val ioStarted = CountDownLatch(1)

        try {
            val dataDir = File(System.getProperty("java.io.tmpdir") ?: "/tmp")
            val freeDiskCollector = DeviceDataCollector(
                Mockito.mock(Connectivity::class.java),
                Mockito.mock(Context::class.java),
                res,
                ValueProvider(DeviceIdStore.DeviceIds("fakeDevice", "internalFakeDevice")),
                Mockito.mock(DeviceBuildInfo::class.java),
                dataDir,
                ValueProvider(false),
                bgTaskService,
                Mockito.mock(Logger::class.java)
            )

            // populate the last known value while the IO executor is healthy
            val initial = freeDiskCollector.calculateFreeDisk()
            Assert.assertTrue(initial > 0)

            // block the single IO thread
            bgTaskService.submitTask(
                TaskType.IO,
                Runnable {
                    ioStarted.countDown()
                    release.await()
                }
            )
            Assert.assertTrue(ioStarted.await(1, TimeUnit.SECONDS))

            val start = System.nanoTime()
            val result = freeDiskCollector.calculateFreeDisk()
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)

            Assert.assertEquals(initial, result)
            Assert.assertTrue(
                "calculateFreeDisk took ${elapsedMs}ms",
                elapsedMs < FREE_DISK_TIMEOUT_MS + 1000
            )
        } finally {
            release.countDown()
            bgTaskService.shutdown()
        }
    }
}
