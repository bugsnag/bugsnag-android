package com.bugsnag.android.mazerunner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.scenarios.Scenario
import java.io.File

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

import kotlin.concurrent.thread

import org.json.JSONObject

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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val closeDialog = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeDialog)
        }

        // Clear persistent data (used to stop scenarios bleeding into each other)
        findViewById<Button>(R.id.clear_persistent_data).setOnClickListener {
            clearFolder("last-run-info")
            clearFolder("bugsnag-errors")
            clearFolder("device-id")
            clearFolder("user-info")
            clearFolder("fake")
        }

        // Get the next maze runner command
        findViewById<Button>(R.id.run_command).setOnClickListener {
            thread(start = true) {
                // Get the next command from Maze Runner
                val commandUrl: String = "http://bs-local.com:9339/command"
                var command: JSONObject
                try {
                    val url = URL(commandUrl)
                    val sb = StringBuilder()
                    val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
                    conn.setRequestMethod("GET")
                    val reader = BufferedReader(InputStreamReader(conn.getInputStream()))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line)
                    }
                    val commandStr = sb.toString()
                    log("Received command: " + commandStr)
                    command = JSONObject(commandStr)

                    val action = command.getString("action")
                    val scenarioName = command.getString("scenario_name")
                    val scenarioMode = command.getString("scenario_mode")
                    log("command.action: " + action)
                    log("command.scenario_name: " + scenarioName)
                    log("command.scenario_mode: " + scenarioMode)

                    runOnUiThread {
                        when (action) {
                            "start_bugsnag" -> start_bugsnag(scenarioName, scenarioMode)
                            "run_scenario" -> run_scenario(scenarioName, scenarioMode)
                            else -> throw Exception("Unknown action: " + action)
                        }
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
    private fun start_bugsnag(eventType: String, mode: String) {
        scenario = loadScenario(eventType, mode)
        scenario?.startBugsnag(true)
    }

    // execute the pre-loaded scenario, or load it then execute it if needed
    private fun run_scenario(eventType: String, mode: String) {
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
