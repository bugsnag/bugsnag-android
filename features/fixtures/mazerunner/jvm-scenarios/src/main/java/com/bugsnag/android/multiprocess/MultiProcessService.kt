package com.bugsnag.android.mazerunner.multiprocess

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.BugsnagIntentParams
import com.bugsnag.android.mazerunner.MazerunnerHttpClient
import com.bugsnag.android.mazerunner.log
import com.bugsnag.android.mazerunner.prepareConfig
import com.bugsnag.android.mazerunner.scenarios.Scenario
import java.io.File
import java.net.URL

class MultiProcessService : Service() {

    var scenario: Scenario? = null

    override fun onBind(intent: Intent) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val params = BugsnagIntentParams.decode(intent)
            log("Multiprocess service received start command: $params")
            sendBroadcast(Intent(ACTION_LAUNCHED_MULTI_PROCESS))

            // start work in a background thread like regular services
            val handlerThread = HandlerThread("multi-process-service")
            handlerThread.start()
            Handler(handlerThread.looper).post {
                if (params.eventType == null) { // load bugsnag to deliver previous events
                    loadBugsnag(params)
                } else { // run code that generates errors/sessions
                    runScenario(params)
                }
            }
        } else {
            log("Multiprocess service has no intent, possible restart?")
        }
        return START_NOT_STICKY
    }

    private fun loadBugsnag(params: BugsnagIntentParams) {
        val config = prepareServiceConfig(params)
        Bugsnag.start(this, config)
    }

    private fun runScenario(params: BugsnagIntentParams) {
        val config = prepareServiceConfig(params)

        scenario = Scenario.load(this, config, params.eventType!!, params.eventMetadata).apply {
            startBugsnag(false)
            log("Executing scenario")
            startScenario()
        }
    }

    private fun prepareServiceConfig(params: BugsnagIntentParams): Configuration {
        // send HTTP requests for intercepted log messages and metrics from Bugsnag.
        // reuse notify endpoint as we don't care about logs when running mazerunner in manual mode
        val logEndpoint = URL(params.notify.replace("/notify", "/logs"))
        val metricsEndpoint = URL(params.notify.replace("/notify", "/metrics"))
        val mazerunnerHttpClient = MazerunnerHttpClient(logEndpoint, metricsEndpoint)

        val config = prepareConfig(
            params.apiKey,
            params.notify,
            params.sessions,
            mazerunnerHttpClient
        ) {
            scenario?.getInterceptedLogMessages()?.contains(it) ?: false
        }
        config.persistenceDirectory = File(filesDir, "background-service-dir")
        return config
    }

    companion object {
        internal const val ACTION_LAUNCHED_MULTI_PROCESS = "ACTION_LAUNCHED_MULTI_PROCESS"
    }
}
