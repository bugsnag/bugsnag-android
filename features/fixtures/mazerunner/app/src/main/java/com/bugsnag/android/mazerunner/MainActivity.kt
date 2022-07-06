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
    var polling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log("MainActivity.onCreate called")
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        prefs = getPreferences(Context.MODE_PRIVATE)

        // Attempt to dismiss any system dialogs (such as "MazeRunner crashed")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            log("Broadcast ACTION_CLOSE_SYSTEM_DIALOGS intent")
            val closeDialog = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeDialog)
        }

        log("Set up clearUserData click handler")
        val clearUserData = findViewById<Button>(R.id.clearUserData)
        clearUserData.setOnClickListener {
            clearStoredApiKey()
            val apiKeyField = findViewById<EditText>(R.id.manualApiKey)
            apiKeyField.text.clear()
            log("Cleared user data")
        }

        if (apiKeyStored()) {
            log("Using stored API key")
            val apiKey = getStoredApiKey()
            val apiKeyField = findViewById<EditText>(R.id.manualApiKey)
            apiKeyField.text.clear()
            apiKeyField.text.append(apiKey)
        }
        log("MainActivity.onCreate complete")
    }

    override fun onResume() {
        super.onResume()
        log("MainActivity.onResume called")

        if (!polling) {
            startCommandRunner()
        }
        log("MainActivity.onResume complete")
    }

    // Checks general internet and secure tunnel connectivity
    private fun checkNetwork() {
        log("Checking network connectivity")
        try {
            URL("https://www.google.com").readText()
            log("Connection to www.google.com seems ok")
        } catch (e: Exception) {
            log("Connection to www.google.com FAILED", e)
        }

        try {
            URL("http://local:9339").readText()
            log("Connection to Maze Runner seems ok")
        } catch (e: Exception) {
            log("Connection to Maze Runner FAILED", e)
        }
    }

    // Starts a thread to poll for Maze Runner actions to perform
    private fun startCommandRunner() {
        // Get the next maze runner command
        polling = true
        thread(start = true) {
            checkNetwork()

            while (polling) {
                Thread.sleep(1000)
                try {
                    // Get the next command from Maze Runner
                    val commandUrl: String = "http://local:9339/command"
                    val commandStr = URL(commandUrl).readText()
                    if (commandStr == "null") {
                        log("No Maze Runner commands queued")
                        continue
                    }

                    // Log the received command
                    log("Received command: $commandStr")
                    var command = JSONObject(commandStr)
                    val action = command.getString("action")
                    val scenarioName = command.getString("scenario_name")
                    val scenarioMode = command.getString("scenario_mode")
                    val sessionsUrl = command.getString("sessions_endpoint")
                    val notifyUrl = command.getString("notify_endpoint")
                    log("command.action: $action")
                    log("command.scenarioName: $scenarioName")
                    log("command.scenarioMode: $scenarioMode")
                    log("command.sessionsUrl: $sessionsUrl")
                    log("command.notifyUrl: $notifyUrl")

                    runOnUiThread {
                        // Display some feedback of the action being run on he UI
                        val actionField = findViewById<EditText>(R.id.command_action)
                        val scenarioField = findViewById<EditText>(R.id.command_scenario)
                        actionField.setText(action)
                        scenarioField.setText(scenarioName)

                        // Perform the given action on the UI thread
                        when (action) {
                            "start_bugsnag" -> {
                                polling = false
                                startBugsnag(scenarioName, scenarioMode, sessionsUrl, notifyUrl)
                            }
                            "run_scenario" -> {
                                polling = false
                                runScenario(scenarioName, scenarioMode, sessionsUrl, notifyUrl)
                            }
                            "clear_persistent_data" -> clearPersistentData()
                            else -> throw IllegalArgumentException("Unknown action: $action")
                        }
                    }
                } catch (e: Exception) {
                    log("Failed to fetch command from Maze Runner", e)
                }
            }
        }
    }

    // load the scenario first, which initialises bugsnag without running any crashy code
    private fun startBugsnag(eventType: String, mode: String, sessionsUrl: String, notifyUrl: String) {
        scenario = loadScenario(eventType, mode, sessionsUrl, notifyUrl)
        scenario?.startBugsnag(true)
    }

    // execute the pre-loaded scenario, or load it then execute it if needed
    private fun runScenario(eventType: String, mode: String, sessionsUrl: String, notifyUrl: String) {
        if (scenario == null) {
            scenario = loadScenario(eventType, mode, sessionsUrl, notifyUrl)
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

    private fun loadScenario(eventType: String, mode: String, sessionsUrl: String, notifyUrl: String): Scenario {

        val apiKeyField = findViewById<EditText>(R.id.manualApiKey)

        val manualMode = apiKeyField.text.isNotEmpty()
        val apiKey = when {
            manualMode -> apiKeyField.text.toString()
            else -> "a35a2a72bd230ac0aa0f52715bbdc6aa"
        }

        if (manualMode) {
            log("Running in manual mode with API key: $apiKey")
            setStoredApiKey(apiKey)
        }
        val config = prepareConfig(apiKey, notifyUrl, sessionsUrl) {
            var logMessage = it
            val interceptedLogMessages = scenario?.getInterceptedLogMessages()
            interceptedLogMessages?.any {
                Regex(it).matches(logMessage)
            } ?: false
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
