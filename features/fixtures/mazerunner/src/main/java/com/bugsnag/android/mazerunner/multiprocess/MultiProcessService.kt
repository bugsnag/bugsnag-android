package com.bugsnag.android.mazerunner.multiprocess

import android.app.Service
import android.content.Intent
import com.bugsnag.android.mazerunner.BugsnagIntentParams
import com.bugsnag.android.mazerunner.log
import com.bugsnag.android.mazerunner.prepareConfig
import com.bugsnag.android.mazerunner.scenarios.Scenario
import java.io.File

class MultiProcessService : Service() {

    override fun onBind(intent: Intent) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val params = BugsnagIntentParams.decode(intent)
            log("Multiprocess service received start command: $params")
            runScenario(params)
        } else {
            log("Multiprocess service has no intent, possible restart?")
        }
        return START_NOT_STICKY
    }

    private fun runScenario(params: BugsnagIntentParams) {
        val config = prepareConfig(params.apiKey, params.notify, params.sessions)
        config.persistenceDirectory = File(filesDir, "background-service-dir")

        Scenario.load(this, config, params.eventType, params.eventMetadata).apply {
            startBugsnag()
            log("Executing scenario")
            startScenario()
        }
    }
}
