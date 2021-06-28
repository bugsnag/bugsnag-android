package com.bugsnag.android.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bugsnag.android.EventHooks
import com.bugsnag.android.JsonStream
import com.bugsnag.android.generateSession
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

/**
 * Benchmarks the performance of serializing error/session payloads to JSON.
 */
@RunWith(AndroidJUnit4::class)
class JsonSerializationBenchmarkTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    /**
     * Serializes an event payload to JSON
     */
    @Test
    fun serializeEventPayload() {
        val payload = EventHooks.generateEvent()

        benchmarkRule.measureRepeated {
            val stream = benchmarkRule.scope.runWithTimingDisabled {
                JsonStream(PrintWriter(ByteArrayOutputStream()).buffered())
            }
            stream.use {
                payload?.toStream(stream)
            }
        }
    }

    /**
     * Serializes a session payload to JSON
     */
    @Test
    fun serializeSessionPayload() {
        val payload = generateSession()

        benchmarkRule.measureRepeated {
            val stream = benchmarkRule.scope.runWithTimingDisabled {
                JsonStream(PrintWriter(ByteArrayOutputStream()).buffered())
            }
            stream.use {
                payload.toStream(stream)
            }
        }
    }
}
