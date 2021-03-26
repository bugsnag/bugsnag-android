package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends an exception with a long stacktrace to Bugsnag, which should trim the stack frames so that
 * the request can be sent
 */
internal class TrimmedStacktraceScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    override fun startScenario() {
        super.startScenario()
        val stacktrace = mutableListOf<StackTraceElement>()

        for (lineNumber in 1..100000) {
            stacktrace.add(
                StackTraceElement(
                    "SomeClass",
                    "someRecursiveMethod",
                    "Foo.kt",
                    lineNumber
                )
            )
        }
        val exc = RuntimeException()
        exc.stackTrace = stacktrace.toTypedArray()

        Bugsnag.notify(exc) {
            it.errors[0].errorClass = "CustomException"
            it.errors[0].errorMessage = "foo"
            true
        }
    }
}
