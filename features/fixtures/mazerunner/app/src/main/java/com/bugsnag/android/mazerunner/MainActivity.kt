package com.bugsnag.android.mazerunner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.EndpointConfiguration
import com.bugsnag.android.Logger
import com.bugsnag.android.mazerunner.scenarios.Scenario

class MainActivity : Activity() {

    private val apiKeyKey = "BUGSNAG_API_KEY"

    private val factory = TestCaseFactory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        log("Launched mazerunner fixture MainActivity")

        // Attempt to dismiss any system dialogs (such as "MazeRunner crashed")
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

        val clearUserData = findViewById<Button>(R.id.clearUserData)

        clearUserData.setOnClickListener {
            clearStoredApiKey()
            val apiKeyField = findViewById<EditText>(R.id.manualApiKey)
            apiKeyField.text.clear()
            log("Cleared user data")
        }

        if (apiKeyStored()) {
            val apiKey = getStoredApiKey()
            val apiKeyField = findViewById<EditText>(R.id.manualApiKey)
            apiKeyField.text.clear()
            apiKeyField.text.append(apiKey)
        }
    }

    private fun apiKeyStored(): Boolean {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return false
        return sharedPref.contains(apiKeyKey)
    }

    private fun setStoredApiKey(apiKey: String) {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putString(apiKeyKey, apiKey)
            commit()
        }
    }

    private fun clearStoredApiKey() {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            remove(apiKeyKey)
            commit()
        }
    }

    private fun getStoredApiKey(): String? {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return ""
        return sharedPref.getString(apiKeyKey, "")
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
            log("Executing scenario $eventType")
            testCase.run()
        }, 1)
    }

    private fun loadScenario(configuration: Configuration, eventType: String, eventMetaData: String): Scenario {
        log("Loading scenario $eventType with metadata $eventMetaData")
        this.intent.putExtra("EVENT_METADATA", eventMetaData)
        val testCase = factory.testCaseForName(eventType, configuration, this)
        testCase.eventMetaData = eventMetaData
        return testCase
    }

    private fun prepareConfig(): Configuration {
        val apiKeyField = findViewById<EditText>(R.id.manualApiKey)
        val notifyEndpointField = findViewById<EditText>(R.id.notifyEndpoint)
        val sessionEndpointField = findViewById<EditText>(R.id.sessionEndpoint)
        val config: Configuration
        if (apiKeyField.text.isNotEmpty()) {
            val manualApiKey = apiKeyField.text.toString()
            log("Running in manual mode with API key: $manualApiKey")
            config = Configuration(manualApiKey)
            setStoredApiKey(manualApiKey)
        } else {
            config = Configuration("a35a2a72bd230ac0aa0f52715bbdc6aa")
            config.endpoints = EndpointConfiguration(notifyEndpointField.text.toString(),
                                                     sessionEndpointField.text.toString())
        }
        config.enabledErrorTypes.ndkCrashes = true
        config.enabledErrorTypes.anrs = true
        config.logger = getBugsnagLogger()
        return config
    }

    private fun getBugsnagLogger(): Logger {
        return object: Logger {
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
    }
}

internal fun log(msg: String) {
    Log.d("BugsnagMazeRunner", msg)
}
