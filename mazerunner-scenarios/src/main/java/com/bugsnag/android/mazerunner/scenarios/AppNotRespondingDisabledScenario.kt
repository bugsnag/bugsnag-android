package com.bugsnag.android.mazerunner.scenarios

import android.os.Looper
import android.os.Handler
import android.content.Context
import com.bugsnag.android.Configuration

/**
 * Stops the app from responding for a time period with ANR detection disabled
 */
internal class AppNotRespondingDisabledScenario(config: Configuration,
                                  context: Context) : Scenario(config, context) {
    init {
        config.setAutoCaptureSessions(false)
        config.detectAnrs = false
    }

    override fun run() {
        super.run()
        val main = Handler(Looper.getMainLooper())
        main.postDelayed({
            Thread.sleep(50000) // FOREVER
        }, 1) // A moment of delay so there is something to 'tap' onscreen
    }

}
