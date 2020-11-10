package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends an exception with a long stacktrace to Bugsnag, which should trim the stack frames so that
 * the request can be sent
 */
internal class TrimmedStacktraceScenario(config: Configuration,
                                         context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
    }

    override fun run() {
        super.run()
        val stacktrace = mutableListOf<StackTraceElement>()

        for (lineNumber in 1..100000) {
            stacktrace.add(StackTraceElement("SomeClass",
                "someRecursiveMethod", "Foo.kt", lineNumber))
        }
        val exc = RuntimeException()
        exc.stackTrace = stacktrace.toTypedArray()

        Bugsnag.notify(exc){
            it.errors[0].errorClass = "CustomException"
            it.errors[0].errorMessage = "foo"
            true
        }
    }

}
