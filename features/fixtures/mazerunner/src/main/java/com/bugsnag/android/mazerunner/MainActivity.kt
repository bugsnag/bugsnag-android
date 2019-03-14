package com.bugsnag.android.mazerunner

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.scenarios.Scenario

class MainActivity : Activity() {

    private val factory = TestCaseFactory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val config = prepareConfig()
        val testCase = loadScenario(config)

        Bugsnag.init(this, config)
        Bugsnag.setLoggingEnabled(true)

        /**
         * Enqueues the test case with a delay on the main thread. This avoids the Activity wrapping
         * unhandled Exceptions
         */
        window.decorView.postDelayed({
            testCase.run()
        }, 1)
    }

    private fun loadScenario(configuration: Configuration): Scenario {
        val eventType = intent.getStringExtra("EVENT_TYPE")
        val eventMetaData = intent.getStringExtra("EVENT_METADATA")
        Log.d("Bugsnag", "Received test case, executing " + eventType)

        val testCase = factory.testCaseForName(eventType, configuration, this)
        testCase.eventMetaData = eventMetaData

        return testCase
    }

    private fun prepareConfig(): Configuration {
        val config = Configuration(intent.getStringExtra("BUGSNAG_API_KEY"))
        val port = intent.getStringExtra("BUGSNAG_PORT")
        config.setEndpoints("${findHostname()}:$port", "${findHostname()}:$port")
        config.detectAnrs = false
        return config
    }

    private fun findHostname(): String {
        val isEmulator = Build.FINGERPRINT.startsWith("unknown")
            || Build.FINGERPRINT.contains("generic")
        return when {
            isEmulator -> "http://10.0.2.2"
            else -> "http://localhost"
        }
    }

}
