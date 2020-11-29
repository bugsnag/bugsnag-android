package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends an automated session payload to Bugsnag.
 */
internal class AutoSessionSmokeScenario(config: Configuration,
                                        context: Context) : Scenario(config, context) {
    override fun run() {
        super.run()
        config.autoTrackSessions = true
        Bugsnag.start(context, config)
        context.startActivity(Intent("com.bugsnag.android.mazerunner.UPDATE_CONTEXT"))

        val main = Handler(Looper.getMainLooper())
        main.postDelayed(Runnable {
            Bugsnag.notify(generateException())
        }, 1000)
    }

}
