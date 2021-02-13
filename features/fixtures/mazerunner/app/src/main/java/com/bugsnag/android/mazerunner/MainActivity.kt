package com.bugsnag.android.mazerunner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.scenarios.Scenario

class MainActivity : Activity() {

    private val apiKeyKey = "BUGSNAG_API_KEY"
    lateinit var prefs: SharedPreferences

    var scenario: Scenario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        log("Launched mazerunner fixture MainActivity")
        prefs = getPreferences(Context.MODE_PRIVATE)

        // Attempt to dismiss any system dialogs (such as "MazeRunner crashed")
        val closeDialog = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        sendBroadcast(closeDialog)

        // load the scenario first, which initialises bugsnag without running any crashy code
        findViewById<Button>(R.id.start_bugsnag).setOnClickListener {
            scenario = loadScenarioFromUi()
            scenario?.startBugsnag(true)

        }

        // execute the pre-loaded scenario, or load it then execute it if needed
        findViewById<Button>(R.id.run_scenario).setOnClickListener {
            if (scenario == null) {
                scenario = loadScenarioFromUi()
                scenario?.startBugsnag(false)
            }

            /**
             * Enqueues the test case with a delay on the main thread. This avoids the Activity wrapping
             * unhandled Exceptions
             */
            window.decorView.postDelayed(
                {
                    log("Executing scenario")
                    scenario?.startScenario()
                },
                1
            )
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

    private fun loadScenarioFromUi(): Scenario {
        val scenarioPicker = findViewById<EditText>(R.id.scenario_name)
        val eventType = scenarioPicker.text.toString()
        val eventMetadata = findViewById<EditText>(R.id.scenario_metadata)
        val metadata = eventMetadata.text.toString()

        val config = loadConfigFromUi()
        return Scenario.load(this, config, eventType, metadata)
    }

    private fun loadConfigFromUi(): Configuration {
        val apiKeyField = findViewById<EditText>(R.id.manualApiKey)
        val notifyEndpointField = findViewById<EditText>(R.id.notify_endpoint)
        val sessionEndpointField = findViewById<EditText>(R.id.session_endpoint)

        val manualMode = apiKeyField.text.isNotEmpty()
        val apiKey = when {
            manualMode -> apiKeyField.text.toString()
            else -> "a35a2a72bd230ac0aa0f52715bbdc6aa"
        }

        if (manualMode) {
            log("Running in manual mode with API key: $apiKey")
            setStoredApiKey(apiKey)
        }
        val notify = notifyEndpointField.text.toString()
        val sessions = sessionEndpointField.text.toString()
        return prepareConfig(apiKey, notify, sessions) {
            val interceptedLogMessages = scenario?.getInterceptedLogMessages()
            interceptedLogMessages?.contains(it) ?: false
        }
    }

    private fun apiKeyStored() = prefs.contains(apiKeyKey)

    private fun setStoredApiKey(apiKey: String) {
        with(prefs.edit()) {
            putString(apiKeyKey, apiKey)
            commit()
        }
    }

    private fun clearStoredApiKey() {
        with(prefs.edit()) {
            remove(apiKeyKey)
            commit()
        }
    }

    private fun getStoredApiKey() = prefs.getString(apiKeyKey, "")
}
