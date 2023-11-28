package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.Telemetry

class EventTooBigScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    val breadcrumbSize: Int
    val breadcrumbCount: Int
    val crashType: String

    init {
        System.loadLibrary("bugsnag-ndk")
        System.loadLibrary("cxx-scenarios-bugsnag")

        if (eventMetadata.isEmpty()) {
            crashType = "error: unconfigured"
            breadcrumbSize = 1
            breadcrumbCount = 1
        } else {
            // Args: crash type (handled, java, native), breadcrumbSize (int), breadcrumbCount (int)
            val args = eventMetadata.split(",").map { it.trim() }
            val expectedArgs = 3
            if (args.size != expectedArgs) {
                throw IllegalArgumentException(
                    "Maze Runner configuration error: Expected $expectedArgs arguments in" +
                        " eventMetadata but got ${args.size}"
                )
            }

            crashType = args[0]
            breadcrumbSize = args[1].toInt()
            breadcrumbCount = args[2].toInt()
            config.setTelemetry(config.getTelemetry() + Telemetry.USAGE)
        }
        // Remove all threads from the error as the stack traces vary in size enough to trigger
        // different trimming behavior, and cause the scenarios to fail
        config.addOnError { event ->
            event.threads.clear()
            true
        }
    }

    external fun nativeCrash(value: Int): Int

    private fun jvmCrash() {
        listOf<Int>()[0]
    }

    private fun crash() {
        when (crashType) {
            "handled" -> Bugsnag.notify(generateException())
            "jvm" -> jvmCrash()
            "native" -> nativeCrash(1)
            else -> throw IllegalArgumentException(
                "Maze Runner configuration error: Unknown crash type \"${crashType}\""
            )
        }
    }

    override fun startScenario() {
        super.startScenario()

        repeat(breadcrumbCount) {
            Bugsnag.leaveBreadcrumb("$it" + "*".repeat(breadcrumbSize))
        }
        crash()
    }
}
