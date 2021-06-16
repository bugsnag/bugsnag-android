package com.bugsnag.android.benchmark

import android.app.Application
import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bugsnag.android.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class JournalBenchmarkTest {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    lateinit var client: Client
    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext<Application>()
        client = generateClient(ctx)
    }

    @Test
    fun serializeEventPayload() {
        val journal = Journal()
        val entries = listOf<Journal.Command>(
            Journal.Command("a", "b")
        )

        benchmarkRule.measureRepeated {
            journal.clear()
            val stream = benchmarkRule.scope.runWithTimingDisabled {
                ByteArrayOutputStream()
            }
            stream.use {
                for(entry in entries) {
                    journal.add(entry)
                }
                journal.serialize(stream)
            }
        }
    }
}
