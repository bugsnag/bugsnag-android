package com.bugsnag.android.benchmark

import android.app.Application
import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bugsnag.android.Client
import com.bugsnag.android.generateClient
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmarks the performance of Bugsnag APIs for setting the user/context.
 */
@RunWith(AndroidJUnit4::class)
class ClientDataBenchmarkTest {

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
     * Alters the user information
     */
    @Test
    fun setUser() {
        benchmarkRule.measureRepeated {
            client.setUser("123", "jane@fake.com", "Jane Eyre")
        }
    }

    /**
     * Sets the error context
     */
    @Test
    fun setContext() {
        benchmarkRule.measureRepeated {
            client.context = "Something went wrong"
        }
    }
}
