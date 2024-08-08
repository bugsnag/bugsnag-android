package com.bugsnag.android.mazerunner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.widget.Button
import android.widget.EditText
import com.bugsnag.android.BugsnagInternals
import com.bugsnag.android.mazerunner.scenarios.Scenario
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.max

const val CONFIG_FILE_TIMEOUT = 5000

class MainActivity : Activity() {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val apiKeyKey = "BUGSNAG_API_KEY"
    lateinit var prefs: SharedPreferences

    var scenario: Scenario? = null
    var polling = false
    var mazeAddress: String? = null

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

    private fun setMazeRunnerAddress() {
        val context = applicationContext
        val externalFilesDir = context.getExternalFilesDir(null)
        val configFile = File(externalFilesDir, "fixture_config.json")
        CiLog.info("Attempting to read Maze Runner address from ${configFile.path}")

        // Poll for the fixture config file
        val pollEnd = System.currentTimeMillis() + CONFIG_FILE_TIMEOUT
        while (System.currentTimeMillis() < pollEnd) {
            if (configFile.exists()) {
                val fileContents = configFile.readText()
                val fixtureConfig = runCatching { JSONObject(fileContents) }.getOrNull()
                mazeAddress = getStringSafely(fixtureConfig, "maze_address")
                if (!mazeAddress.isNullOrBlank()) {
                    CiLog.info("Maze Runner address set from config file: $mazeAddress")
                    break
                }
            }

            Thread.sleep(250)
        }

        // Assume we are running in legacy mode on BrowserStack
        if (mazeAddress.isNullOrBlank()) {
            CiLog.warn("Failed to read Maze Runner address from config file, defaulting to legacy BrowserStack address")
            mazeAddress = "bs-local.com:9339"
        }
    }

    // Checks general internet and secure tunnel connectivity
    private fun checkNetwork() {
        CiLog.info("Network connectivity: $networkStatus")
        try {
            URL("http://$mazeAddress").readText()
            CiLog.info("Connection to Maze Runner seems ok")
        } catch (e: Exception) {
            CiLog.error("Connection to Maze Runner FAILED", e)
        }
    }

    // As per JSONObject.getString but returns and empty string rather than throwing if not present
    private fun getStringSafely(jsonObject: JSONObject?, key: String): String {
        return jsonObject?.optString(key) ?: ""
    }

    // Starts a thread to poll for Maze Runner actions to perform
    private fun startCommandRunner() {
        // Get the next maze runner command
        polling = true
        thread(start = true) {
            if (mazeAddress == null) setMazeRunnerAddress()
            checkNetwork()

            while (polling) {
                Thread.sleep(1000)
                try {
                    // Get the next command from Maze Runner
                    val commandStr = readCommand()
                    if (commandStr == "null") {
                        CiLog.info("No Maze Runner commands queued")
                        continue
                    }

                    // Log the received command
                    CiLog.info("Received command: $commandStr")
                    var command = JSONObject(commandStr)
                    val action = getStringSafely(command, "action")
                    val scenarioName = getStringSafely(command, "scenario_name")
                    val scenarioMode = getStringSafely(command, "scenario_mode")
                    val sessionsUrl = getStringSafely(command, "sessions_endpoint")
                    val notifyUrl = getStringSafely(command, "notify_endpoint")
                    log("command.action: $action")
                    log("command.scenarioName: $scenarioName")
                    log("command.scenarioMode: $scenarioMode")
                    log("command.sessionsUrl: $sessionsUrl")
                    log("command.notifyUrl: $notifyUrl")

                    // Stop polling once we have a scenario action
                    if ("start_bugsnag".equals(action) || "run_scenario".equals(action)) {
                        polling = false
                    }

                    mainHandler.post {
                        // Display some feedback of the action being run on he UI
                        val actionField = findViewById<EditText>(R.id.command_action)
                        val scenarioField = findViewById<EditText>(R.id.command_scenario)
                        actionField.setText(action)
                        scenarioField.setText(scenarioName)

                        // Perform the given action on the UI thread
                        when (action) {
                            "noop" -> {
                                CiLog.info("No Maze Runner command queuing, continuing to poll")
                            }
                            "start_bugsnag" -> {
                                startBugsnag(scenarioName, scenarioMode, sessionsUrl, notifyUrl)
                            }
                            "run_scenario" -> {
                                runScenario(scenarioName, scenarioMode, sessionsUrl, notifyUrl)
                            }
                            "clear_persistent_data" -> clearPersistentData()
                            "flush" -> BugsnagInternals.flush()
                            else -> throw IllegalArgumentException("Unknown action: $action")
                        }
                    }
                } catch (e: Exception) {
                    CiLog.error("Failed to fetch command from Maze Runner", e)
                }
            }
        }
    }

    private fun readCommand(): String {
        val commandUrl = "http://$mazeAddress/command"
        val urlConnection = URL(commandUrl).openConnection() as HttpURLConnection
        try {
            return urlConnection.inputStream.use { it.reader().readText() }
        } catch (ioe: IOException) {
            CiLog.error("Read of Maze Runner command failed", ioe)
            try {
                val errorMessage = urlConnection.errorStream.use { it.reader().readText() }
                CiLog.error(
                    "Failed to GET $commandUrl (HTTP ${urlConnection.responseCode} " +
                        "${urlConnection.responseMessage}):\n" +
                        "${"-".repeat(errorMessage.width)}\n" +
                        "$errorMessage\n" +
                        "-".repeat(errorMessage.width)
                )
            } catch (e: Exception) {
                log("Failed to retrieve error message from connection", e)
            }

            throw ioe
        }
    }

    // load the scenario first, which initialises bugsnag without running any crashy code
    private fun startBugsnag(
        eventType: String,
        mode: String,
        sessionsUrl: String,
        notifyUrl: String
    ) {
        scenario = loadScenario(eventType, mode, sessionsUrl, notifyUrl)
        scenario?.startBugsnag(true)
    }

    // execute the pre-loaded scenario, or load it then execute it if needed
    private fun runScenario(
        eventType: String,
        mode: String,
        sessionsUrl: String,
        notifyUrl: String
    ) {
        if (scenario == null) {
            scenario = loadScenario(eventType, mode, sessionsUrl, notifyUrl)
            scenario?.startBugsnag(false)
        }

        /**
         * Enqueues the test case with a delay on the main thread. This avoids the Activity wrapping
         * unhandled Exceptions
         */
        mainHandler.post {
            CiLog.info("Executing scenario")
            scenario?.startScenario()
        }
    }

    // Clear persistent data (used to stop scenarios bleeding into each other)
    private fun clearPersistentData() {
        CiLog.info("Clearing persistent data")
        clearCacheFolder("bugsnag")
        clearCacheFolder("StrictModeDiscScenarioFile")
        clearFilesFolder("background-service-dir")

        removeFile("device-id")
        removeFile("internal-device-id")

        listFolders()
    }

    // Recursively deletes the contents of a folder beneath /cache
    private fun clearCacheFolder(name: String) {
        val folder = File(applicationContext.cacheDir, name)
        log("Clearing folder: ${folder.path}")
        folder.deleteRecursively()
    }

    private fun clearFilesFolder(name: String) {
        val folder = File(applicationContext.filesDir, name)
        log("Clearing folder: ${folder.path}")
        folder.deleteRecursively()
    }

    // Deletes a file beneath /files
    private fun removeFile(name: String) {
        val file = File(applicationContext.filesDir, name)
        log("Removing file: ${file.path}")
        file.delete()
    }

    // Logs out the contents of the /cache and /files folders
    private fun listFolders() {
        log("Contents of: ${applicationContext.cacheDir}")
        applicationContext.cacheDir.walkTopDown().forEach {
            log(it.absolutePath)
        }

        log("Contents of: ${applicationContext.filesDir}")
        applicationContext.filesDir.walkTopDown().forEach {
            log(it.absolutePath)
        }
    }

    private fun loadScenario(
        eventType: String,
        mode: String,
        sessionsUrl: String,
        notifyUrl: String
    ): Scenario {
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

        // send HTTP requests for intercepted log messages and metrics from Bugsnag.
        // reuse notify endpoint as we don't care about logs when running mazerunner in manual mode
        val mazerunnerHttpClient = MazerunnerHttpClient.fromEndpoint(notifyUrl)

        val config = prepareConfig(apiKey, notifyUrl, sessionsUrl, mazerunnerHttpClient) {
            val logMessage = it
            val interceptedLogMessages = scenario?.getInterceptedLogMessages()
            interceptedLogMessages?.any {
                Regex(it).matches(logMessage)
            } ?: false
        }
        return Scenario.load(this, config, eventType, mode, mazerunnerHttpClient)
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

    private val String.width
        get() =
            lineSequence().fold(0) { maxWidth, line -> max(maxWidth, line.length) }
}
