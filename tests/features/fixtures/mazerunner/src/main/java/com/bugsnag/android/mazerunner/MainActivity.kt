package com.bugsnag.android.mazerunner

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.EndpointConfiguration
import com.bugsnag.android.Logger
import com.bugsnag.android.mazerunner.scenarios.Scenario
import android.content.Intent




class MainActivity : Activity() {

    private val factory = TestCaseFactory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val closeDialog = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        sendBroadcast(closeDialog)

        val bugsnagStarter = findViewById<Button>(R.id.startBugsnagButton)

        bugsnagStarter.setOnClickListener {
            val scenarioPicker = findViewById<EditText>(R.id.scenarioText)
            val scenario = scenarioPicker.text.toString()
            val scenarioMetaData = findViewById<EditText>(R.id.scenarioMetaData)
            val metaData = scenarioMetaData.text.toString()
            startBugsnag(scenario, metaData)
        }

        val scenarioStarter = findViewById<Button>(R.id.startScenarioButton)

        scenarioStarter.setOnClickListener {
            val scenarioPicker = findViewById<EditText>(R.id.scenarioText)
            val scenario = scenarioPicker.text.toString()
            val scenarioMetaData = findViewById<EditText>(R.id.scenarioMetaData)
            val metaData = scenarioMetaData.text.toString()
            executeScenario(scenario, metaData)
        }
    }

    private fun startBugsnag(eventType: String, metaData: String) {
        val config = prepareConfig()
        loadScenario(config, eventType, metaData)

        Bugsnag.start(this, config)
    }

    private fun executeScenario(eventType: String, metaData: String) {
        val config = prepareConfig()
        val testCase = loadScenario(config, eventType, metaData)

        if (metaData != "skipBugsnag") {
            Bugsnag.start(this, config)
        }

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
        Log.d("Bugsnag", "Received metadata: " + eventMetaData)
        this.intent.putExtra("EVENT_METADATA", eventMetaData)
        val testCase = factory.testCaseForName(eventType, configuration, this)
        testCase.eventMetaData = eventMetaData
        return testCase
    }

    private fun prepareConfig(): Configuration {
        val config = Configuration("a35a2a72bd230ac0aa0f52715bbdc6aa")
        config.endpoints = EndpointConfiguration("http://bs-local.com:9339", "http://bs-local.com:9339")
        config.enabledErrorTypes.ndkCrashes = true
        config.enabledErrorTypes.anrs = true
        config.logger = object: Logger {
            private val TAG = "Bugsnag"

            override fun e(msg: String) {
                Log.e(TAG, msg)
            }

            override fun e(msg: String, throwable: Throwable) {
                Log.e(TAG, msg, throwable)
            }

            override fun w(msg: String) {
                Log.w(TAG, msg)
            }

            override fun w(msg: String, throwable: Throwable) {
                Log.w(TAG, msg, throwable)
            }

            override fun i(msg: String) {
                Log.i(TAG, msg)
            }

            override fun i(msg: String, throwable: Throwable) {
                Log.i(TAG, msg, throwable)
            }

            override fun d(msg: String) {
                Log.d(TAG, msg)
            }

            override fun d(msg: String, throwable: Throwable) {
                Log.d(TAG, msg, throwable)
            }
        }
        return config
    }

}
