package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.log
import java.io.File
import java.util.UUID

/**
 * Persists a legacy bsg_event struct on disk. When Bugsnag initializes this should be delivered
 * as an Event.
 */
class CXXPersistLegacyStructScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    external fun persistStruct(path: String)

    init {
        System.loadLibrary("cxx-scenarios-bugsnag")
        val parent = File(context.cacheDir, "bugsnag-native")
        parent.mkdirs()
        check(parent.isDirectory) { "bugsnag-native directory not setup correctly" }
        val eventPath = File(parent, "${UUID.randomUUID()}.crash").absolutePath

        log("Creating legacy struct on disk before Bugsnag initializes")
        persistStruct(eventPath)
        log("Created legacy struct at $eventPath")
    }
}
