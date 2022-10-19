package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

class MetadataStringsTooLargeScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    val metadataTestValue: String
    val crashType: String

    init {
        System.loadLibrary("bugsnag-ndk")
        System.loadLibrary("cxx-scenarios-bugsnag")

        if (eventMetadata.isEmpty()) {
            crashType = "error: unconfigured"
            metadataTestValue = ""
        } else {
            // Args: crash type (handled, java, native), maxStringValue (int), testStringLength (int)
            val args = eventMetadata.split(",").map { it.trim() }
            val expectedArgs = 3
            if (args.size != expectedArgs) {
                throw IllegalArgumentException(
                    "Maze Runner configuration error: Expected $expectedArgs arguments in" +
                        " eventMetadata but got ${args.size}: $args"
                )
            }

            crashType = args[0]
            config.maxStringValueLength = args[1].toInt()
            metadataTestValue = "0".repeat(args[2].toInt())
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
        Bugsnag.addMetadata("custom", "foo", metadataTestValue)
        Bugsnag.leaveBreadcrumb("big" + "*".repeat(995000))
        Bugsnag.leaveBreadcrumb(
            "test",
            mutableMapOf<String, Any>("a" to metadataTestValue),
            BreadcrumbType.MANUAL
        )
        crash()
    }
}
