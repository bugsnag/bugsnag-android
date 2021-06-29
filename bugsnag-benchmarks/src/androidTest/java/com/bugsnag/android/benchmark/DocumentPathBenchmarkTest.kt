package com.bugsnag.android.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.bugsnag.android.internal.DocumentPath
import org.junit.Rule
import org.junit.Test

class DocumentPathBenchmarkTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun parseDocumentPath() {
        benchmarkRule.measureRepeated {
            DocumentPath("foo.bar.a.-1.3.")
        }
    }

    @Test
    fun parseDocumentPathWithEscapes() {
        benchmarkRule.measureRepeated {
            DocumentPath("a.b\\\\s\\.a.-1.3.")
        }
    }
}