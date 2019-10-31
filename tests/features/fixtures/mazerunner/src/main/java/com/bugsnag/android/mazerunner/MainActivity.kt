package com.bugsnag.android.mazerunner

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.Endpoints
import com.bugsnag.android.mazerunner.scenarios.Scenario

class MainActivity : Activity() {

    private val factory = TestCaseFactory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bugsnagStarter = findViewById<Button>(R.id.startBugsnagButton)

        bugsnagStarter.setOnClickListener {
            val scenarioPicker = findViewById<EditText>(R.id.scenarioText)
            val scenario = scenarioPicker.text.toString()
            val scenarioMetadata = findViewById<EditText>(R.id.scenarioMetadata)
            val metadata = scenarioMetadata.text.toString()
            startBugsnag(scenario, metadata)
        }

        val scenarioStarter = findViewById<Button>(R.id.startScenarioButton)

        scenarioStarter.setOnClickListener {
            val scenarioPicker = findViewById<EditText>(R.id.scenarioText)
            val scenario = scenarioPicker.text.toString()
            val scenarioMetadata = findViewById<EditText>(R.id.scenarioMetadata)
            val metadata = scenarioMetadata.text.toString()
            executeScenario(scenario, metadata)
        }
    }

    private fun startBugsnag(eventType: String, metadata: String) {
        val config = prepareConfig()
        loadScenario(config, eventType, metadata)

        Bugsnag.init(this, config)
    }

    private fun executeScenario(eventType: String, metadata: String) {
        val config = prepareConfig()
        val testCase = loadScenario(config, eventType, metadata)

        Bugsnag.init(this, config)

        /**
         * Enqueues the test case with a delay on the main thread. This avoids the Activity wrapping
         * unhandled Exceptions
         */
        window.decorView.postDelayed({
            testCase.run()
        }, 1)
    }

    private fun loadScenario(configuration: Configuration, eventType: String, eventMetadata: String): Scenario {
        Log.d("Bugsnag", "Received test case, executing " + eventType)
        Log.d("Bugsnag", "Received metadata: " + eventMetadata)

        this.intent.putExtra("EVENT_METADATA", eventMetadata)
        val testCase = factory.testCaseForName(eventType, configuration, this)

        return testCase
    }

    private fun prepareConfig(): Configuration {
        val config = Configuration("ABCDEFGHIJKLMNOPQRSTUVWXYZ012345")
        config.endpoints = Endpoints("http://bs-local.com:9339", "http://bs-local.com:9339")
        config.autoDetectNdkCrashes = true
        config.autoDetectAnrs = true
        return config
    }

}
