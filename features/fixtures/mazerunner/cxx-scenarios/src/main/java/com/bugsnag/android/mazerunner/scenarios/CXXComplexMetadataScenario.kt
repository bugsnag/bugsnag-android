package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.log
import java.util.Date

/**
 * Sends a native crash to Bugsnag which has complicated metadata values.
 */
internal class CXXComplexMetadataScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    companion object {
        init {
            System.loadLibrary("cxx-scenarios")
        }
    }

    private external fun crash()

    override fun startScenario() {
        super.startScenario()
        val longStr = "Have you tried turning it off and on again and off and on again and off" +
            " and on again and off and on again and off and on again and off and on again" +
            " and on again and off and on again and off and on again and off and on again?"
        val map = mapOf(
            "int_array" to intArrayOf(-5, 1000, 29, Integer.MAX_VALUE, Integer.MIN_VALUE),
            "string_array" to arrayOf("a", "b", "c"),
            "bool_array" to arrayOf(false, true),
            "large_value" to longStr,
            "array_with_null" to arrayOf("x", null),
            "float_array" to arrayOf(5.0f, 19.3f, 5.623f),
            "long" to 1509234098234L,
            "null" to null,
            "date" to Date(1635770803),
            "unknown_type" to context
        )

        Bugsnag.addMetadata("individual_values", map)
        Bugsnag.addMetadata("map_section", "map", map)
        Bugsnag.addMetadata(
            "collection_section", "collection",
            listOf(
                "a", 5, true
            )
        )

        if (Bugsnag.getLastRunInfo() != null) {
            log("Triggering handled JVM event")
            Bugsnag.notify(generateException())
        } else {
            log("Triggering unhandled NDK event")
            crash()
        }
    }
}
