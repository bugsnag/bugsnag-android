package com.bugsnag.android

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import kotlin.concurrent.thread

@RunWith(MockitoJUnitRunner::class)
class DataCollectorTest {

    @Ignore("Disabled until we're able to mock final classes or auto-open classes")
    @Test
    fun testConcurretAccess() {
        val res = Mockito.mock(Resources::class.java)
        Mockito.`when`(res.configuration).thenReturn(Configuration())

        val collector = DeviceDataCollector(
            Mockito.mock(Connectivity::class.java),
            Mockito.mock(Context::class.java),
            res,
            "fakeDevice",
            "internalFakeDevice",
            Mockito.mock(DeviceBuildInfo::class.java),
            File("/tmp/javatest"),
            Mockito.mock(RootDetector::class.java),
            Mockito.mock(BackgroundTaskService::class.java),
            Mockito.mock(Logger::class.java)
        )

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
