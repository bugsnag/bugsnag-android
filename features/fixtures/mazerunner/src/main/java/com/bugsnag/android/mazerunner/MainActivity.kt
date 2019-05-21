package com.bugsnag.android.mazerunner

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.scenarios.Scenario

class MainActivity : Activity() {

    private val factory = TestCaseFactory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val scenarioStarter = findViewById<Button>(R.id.startScenarioButton)

        scenarioStarter.setOnClickListener {
            val scenarioPicker = findViewById<EditText>(R.id.scenarioText)
            val scenario = scenarioPicker.text.toString()
            val scenarioMetaData = findViewById<EditText>(R.id.scenarioMetaData)
            val metaData = scenarioMetaData.text.toString()
            executeScenario(scenario, metaData)
        }
    }

    private fun executeScenario(eventType: String, metaData: String) {
        val config = prepareConfig()
        val testCase = loadScenario(config, eventType, metaData)

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

    private fun loadScenario(configuration: Configuration, eventType: String, eventMetaData: String): Scenario {
        Log.d("Bugsnag", "Received test case, executing " + eventType)

        val testCase = factory.testCaseForName(eventType, configuration, this)
        testCase.eventMetaData = eventMetaData

        return testCase
    }

    private fun prepareConfig(): Configuration {
        val config = Configuration(intent.getStringExtra("BUGSNAG_API_KEY"))
        config.setEndpoints("http://10.0.2.2:9339", "http://10.0.2.2:9339")
        config.detectAnrs = false
        return config
    }

}
