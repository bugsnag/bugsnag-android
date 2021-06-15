package com.bugsnag.android.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bugsnag.android.Configuration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark, which will execute on an Android device.
 *
 * The body of [BenchmarkRule.measureRepeated] is measured in a loop, and Studio will
 * output the result. Modify your code to see how it affects performance.
 */
@RunWith(AndroidJUnit4::class)
class MainApiBenchmarkTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun configConstructor() {
        benchmarkRule.measureRepeated {
            Configuration("your-api-key")
        }
    }
}
