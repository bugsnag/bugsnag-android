package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends an automated session payload to Bugsnag.
 */
internal class AutoSessionSmokeScenario(config: Configuration,
                                        context: Context) : Scenario(config, context) {

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        if (activity.intent.action == "com.bugsnag.android.mazerunner.UPDATE_CONTEXT") {
            Bugsnag.notify(generateException())
        }
    }

    override fun run() {
        super.run()
        registerActivityLifecycleCallbacks()
        context.startActivity(Intent("com.bugsnag.android.mazerunner.UPDATE_CONTEXT"))
    }

}
