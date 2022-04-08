package com.bugsnag.android.mazerunner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.EditText
import com.bugsnag.android.mazerunner.scenarios.Scenario
import org.json.JSONObject
import java.io.File
import java.lang.IllegalArgumentException
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val apiKeyKey = "BUGSNAG_API_KEY"
    lateinit var prefs: SharedPreferences

    var scenario: Scenario? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        log("Launched mazerunner fixture MainActivity")
        prefs = getPreferences(Context.MODE_PRIVATE)

        // Attempt to dismiss any system dialogs (such as "MazeRunner crashed")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val closeDialog = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeDialog)
        }

        // Get the next maze runner command
        findViewById<Button>(R.id.run_command).setOnClickListener {
            thread(start = true) {
                try {
                    // Get the next command from Maze Runner
                    val commandUrl: String = "http://bs-local.com:9339/command"
                    val commandStr = URL(commandUrl).readText()
                    log("Received command: " + commandStr)
                    var command = JSONObject(commandStr)
                    val action = command.getString("action")
                    val scenarioName = command.getString("scenario_name")
                    val scenarioMode = command.getString("scenario_mode")
                    log("command.action: " + action)
                    log("command.scenarioName: " + scenarioName)
                    log("command.scenarioMode: " + scenarioMode)

                    // Perform the given action
                    when (action) {
                        "start_bugsnag" -> startBugsnag(scenarioName, scenarioMode)
                        "run_scenario" -> runScenario(scenarioName, scenarioMode)
                        "clear_persistent_data" -> clearPersistentData()
                        else -> throw IllegalArgumentException("Unknown action: " + action)
                    }
                } catch (e: Exception) {
                    log("Failed to fetch command from Maze Runner", e)
                }
            }
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

    // load the scenario first, which initialises bugsnag without running any crashy code
    private fun startBugsnag(eventType: String, mode: String) {
        scenario = loadScenario(eventType, mode)
        scenario?.startBugsnag(true)
    }

    // execute the pre-loaded scenario, or load it then execute it if needed
    private fun runScenario(eventType: String, mode: String) {
        if (scenario == null) {
            scenario = loadScenario(eventType, mode)
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

    // Clear persistent data (used to stop scenarios bleeding into each other)
    private fun clearPersistentData() {
        log("Clearing persistent data")
        clearFolder("last-run-info")
        clearFolder("bugsnag-errors")
        clearFolder("device-id")
        clearFolder("user-info")
        clearFolder("fake")
    }

    private fun clearFolder(name: String) {
        val context = MazerunnerApp.applicationContext()
        val folder = File(context.cacheDir, name)
        log("Clearing folder: ${folder.path}")
        folder.deleteRecursively()
    }

    private fun loadScenario(eventType: String, mode: String): Scenario {

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
        val config = prepareConfig(apiKey, notify, sessions) {
            val interceptedLogMessages = scenario?.getInterceptedLogMessages()
            interceptedLogMessages?.contains(it) ?: false
        }
        return Scenario.load(this, config, eventType, mode)
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
