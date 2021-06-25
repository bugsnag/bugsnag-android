package com.bugsnag.android.benchmark

import android.app.Application
import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Client
import com.bugsnag.android.Configuration
import com.bugsnag.android.generateClient
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmarks the performance of Bugsnag APIs which are typically on the critical path.
 * For example, calling notify(), leaving breadcrumbs, and altering metadata is likely to happen
 * many times over an application's lifecycle. This means special care should be taken to ensure
 * that these functions are optimized.
 */
@RunWith(AndroidJUnit4::class)
class CriticalPathBenchmarkTest {

    lateinit var client: Client
    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext<Application>()
        client = generateClient(ctx)
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    /**
     * Construct a configuration object from an API key
     */
    @Test
    fun configConstructor() {
        benchmarkRule.measureRepeated {
            Configuration("your-api-key")
        }
    }

    /**
     * Construct a configuration object from the AndroidManifest
     */
    @Test
    fun configManifestLoad() {
        benchmarkRule.measureRepeated {
            Configuration.load(ctx)
        }
    }

    /**
     * Calls Bugsnag.notify() with an exception. This is not an ideal benchmark
     * as it's a fairly complex operation that involves I/O, but at least gives a general feel
     * for how the API is performing.
     */
    @Test
    fun clientNotify() {
        val exc = benchmarkRule.scope.runWithTimingDisabled {
            RuntimeException("Whoops")
        }
        benchmarkRule.measureRepeated {
            client.notify(exc)
        }
    }

    /**
     * Leave a simple breadcrumb on the Client
     */
    @Test
    fun leaveSimpleBreadcrumb() {
        benchmarkRule.measureRepeated {
            client.leaveBreadcrumb("Hello world")
        }
    }

    /**
     * Leave a simple breadcrumb with metadata on the Client
     */
    @Test
    fun leaveComplexBreadcrumb() {
        val data = benchmarkRule.scope.runWithTimingDisabled {
            mapOf(Pair("isLaunching", true))
        }
        benchmarkRule.measureRepeated {
            client.leaveBreadcrumb("Hello world", data, BreadcrumbType.NAVIGATION)
        }
    }

    /**
     * Make a copy of breadcrumbs on the Client (required when generating events)
     */
    @Test
    fun copyBreadcrumbs() {
        repeat(201) { count ->
            client.leaveBreadcrumb("Hello world $count")
        }
        benchmarkRule.measureRepeated {
            client.breadcrumbs
        }
    }

    /**
     * Add a single value to the Client metadata
     */
    @Test
    fun addSingleMetadataValue() {
        benchmarkRule.measureRepeated {
            client.addMetadata("custom", "mykey", "myvalue")
        }
    }

    /**
     * Add a single value to the Client metadata
     */
    @Test
    fun addMetadataSection() {
        val data = benchmarkRule.scope.runWithTimingDisabled {
            mapOf(Pair("mykey", "myvalue"))
        }
        benchmarkRule.measureRepeated {
            client.addMetadata("custom", data)
        }
    }

    /**
     * Get a single value from the Client metadata
     */
    @Test
    fun getSingleMetadataValue() {
        benchmarkRule.scope.runWithTimingDisabled {
            client.addMetadata("custom", "mykey", "myvalue")
        }
        benchmarkRule.measureRepeated {
            client.getMetadata("custom", "mykey")
        }
    }

    /**
     * Get a single value from the Client metadata
     */
    @Test
    fun getMetadataSection() {
        benchmarkRule.scope.runWithTimingDisabled {
            client.addMetadata("custom", "mykey", "myvalue")
        }
        benchmarkRule.measureRepeated {
            client.getMetadata("custom")
        }
    }

    /**
     * Clear a single value from the Client metadata
     */
    @Test
    fun clearSingleMetadataValue() {
        benchmarkRule.scope.runWithTimingDisabled {
            client.addMetadata("custom", "mykey", "myvalue")
        }
        benchmarkRule.measureRepeated {
            client.clearMetadata("custom", "mykey")
        }
    }

    /**
     * Clear a single value from the Client metadata
     */
    @Test
    fun clearMetadataSection() {
        benchmarkRule.scope.runWithTimingDisabled {
            client.addMetadata("custom", "mykey", "myvalue")
        }
        benchmarkRule.measureRepeated {
            client.clearMetadata("custom")
        }
    }
}
