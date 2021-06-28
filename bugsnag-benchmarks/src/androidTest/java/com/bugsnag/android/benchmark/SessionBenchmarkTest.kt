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
 * Benchmarks the performance of Bugsnag session tracking APIs.
 */
@RunWith(AndroidJUnit4::class)
class SessionBenchmarkTest {

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
     * Starts a new session
     */
    @Test
    fun startSession() {
        benchmarkRule.measureRepeated {
            client.startSession()
        }
    }

    /**
     * Pauses a session
     */
    @Test
    fun pauseSession() {
        benchmarkRule.scope.runWithTimingDisabled {
            client.startSession()
        }
        benchmarkRule.measureRepeated {
            client.pauseSession()
        }
    }

    /**
     * Resumes a session
     */
    @Test
    fun resumeSession() {
        benchmarkRule.scope.runWithTimingDisabled {
            client.startSession()
            client.pauseSession()
        }
        benchmarkRule.measureRepeated {
            client.resumeSession()
        }
    }
}
