package com.bugsnag.android

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.bugsnag.android.internal.BackgroundTaskService
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
}
