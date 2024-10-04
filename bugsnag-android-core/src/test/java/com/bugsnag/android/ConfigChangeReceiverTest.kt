package com.bugsnag.android

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.dag.ValueProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@RunWith(MockitoJUnitRunner::class)
internal class ConfigChangeReceiverTest {

    @Mock
    lateinit var connectivity: Connectivity

    @Mock
    lateinit var appContext: Context

    @Mock
    lateinit var resources: Resources

    @Mock
    lateinit var config: Configuration

    @Mock
    lateinit var configUpdate: Configuration

    @Mock
    lateinit var buildInfo: DeviceBuildInfo

    @Mock
    lateinit var dataDirectory: File

    @Mock
    lateinit var rootDetector: RootDetector

    @Mock
    lateinit var bgTaskService: BackgroundTaskService

    private val memoryCallback: (Boolean, Int?) -> Unit = { _, _ -> }

    private fun createDeviceDataCollector(): DeviceDataCollector = DeviceDataCollector(
        connectivity,
        appContext,
        resources,
        ValueProvider(null),
        buildInfo,
        dataDirectory,
        ValueProvider(false),
        bgTaskService,
        NoopLogger
    )

    @Test
    fun testStringMapping() {
        `when`(resources.configuration).thenReturn(config)
        config.orientation = Configuration.ORIENTATION_LANDSCAPE

        val collector = createDeviceDataCollector()
        assertEquals("landscape", collector.getOrientationAsString())

        collector.updateOrientation(Configuration.ORIENTATION_PORTRAIT)
        assertEquals("portrait", collector.getOrientationAsString())

        collector.updateOrientation(Configuration.ORIENTATION_UNDEFINED)
        assertNull(collector.getOrientationAsString())
    }

    @Test
    fun testOrientationTracking() {
        `when`(resources.configuration).thenReturn(config)
        config.orientation = Configuration.ORIENTATION_LANDSCAPE

        val collector = createDeviceDataCollector()
        assertFalse(collector.updateOrientation(Configuration.ORIENTATION_LANDSCAPE))
        assertEquals("landscape", collector.getOrientationAsString())

        assertTrue(collector.updateOrientation(Configuration.ORIENTATION_PORTRAIT))
        assertEquals("portrait", collector.getOrientationAsString())
    }

    @Test
    fun verifyDefaultOrientation() {
        `when`(resources.configuration).thenReturn(config)
        val orientationCb = { old: String?, new: String? ->
            assertNull(old)
            assertEquals("portrait", new)
        }
        config.orientation = Configuration.ORIENTATION_UNDEFINED
        val callbacks = ClientComponentCallbacks(
            createDeviceDataCollector(),
            orientationCb,
            memoryCallback
        )

        configUpdate.orientation = Configuration.ORIENTATION_PORTRAIT
        callbacks.onConfigurationChanged(configUpdate)
    }

    @Test
    fun verifyOrientationChange() {
        `when`(resources.configuration).thenReturn(config)
        val orientationCb = { old: String?, new: String? ->
            assertEquals("portrait", old)
            assertEquals("landscape", new)
        }
        config.orientation = Configuration.ORIENTATION_PORTRAIT
        val callbacks = ClientComponentCallbacks(
            createDeviceDataCollector(),
            orientationCb,
            memoryCallback
        )

        configUpdate.orientation = Configuration.ORIENTATION_LANDSCAPE
        callbacks.onConfigurationChanged(configUpdate)
    }

    @Test
    fun dupeConfigurationChange() {
        var old: String? = null
        var new: String? = null

        val orientationCb = { a: String?, b: String? ->
            old = a
            new = b
        }
        `when`(resources.configuration).thenReturn(config)
        config.orientation = Configuration.ORIENTATION_PORTRAIT
        val callbacks = ClientComponentCallbacks(
            createDeviceDataCollector(),
            orientationCb,
            memoryCallback
        )

        configUpdate.orientation = Configuration.ORIENTATION_PORTRAIT
        callbacks.onConfigurationChanged(configUpdate)
        assertNull(old)
        assertNull(new)
    }
}
