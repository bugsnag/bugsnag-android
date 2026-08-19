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
import com.bugsnag.android.mazerunner.scenarios.Scenario
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.max

const val CONFIG_FILE_TIMEOUT = 15000
private const val MAZE_RUNNER_COMMAND_TIMEOUT_MS = 5000
private const val LEGACY_MAZE_ADDRESS = "bs-local.com:9339"

private data class MazeRunnerCommand(
    val action: String,
    val scenarioName: String,
    val scenarioMode: String,
    val sessionsUrl: String,
    val notifyUrl: String,
    val remoteConfigUrl: String,
    val commandUUID: String
)

private fun parseMazeRunnerCommand(commandStr: String) = MazeRunnerCommand(
    action = JSONObject(commandStr).optString("action"),
    scenarioName = JSONObject(commandStr).optString("scenario_name"),
    scenarioMode = JSONObject(commandStr).optString("scenario_mode"),
    sessionsUrl = JSONObject(commandStr).optString("sessions_endpoint"),
    notifyUrl = JSONObject(commandStr).optString("notify_endpoint"),
    remoteConfigUrl = JSONObject(commandStr).optString("error_config_endpoint"),
    commandUUID = JSONObject(commandStr).optString("uuid")
)

private fun readMazeRunnerAddressFromConfig(configFile: File): String? {
    if (!configFile.exists()) {
        return null
    }

    val fileContents = configFile.readText()
    val fixtureConfig = runCatching { JSONObject(fileContents) }.getOrNull()
    return fixtureConfig?.optString("maze_address").orEmpty().takeIf { it.isNotBlank() }
}

private fun SharedPreferences.setStoredApiKey(apiKeyKey: String, apiKey: String) {
    with(edit()) {
        putString(apiKeyKey, apiKey)
        commit()
    }
}

private fun SharedPreferences.clearStoredApiKey(apiKeyKey: String) {
    with(edit()) {
        remove(apiKeyKey)
        commit()
    }
}

private fun SharedPreferences.getStoredApiKey(apiKeyKey: String): String? {
    return getString(apiKeyKey, "")
}

private val String.width
    get() =
        lineSequence().fold(0) { maxWidth, line -> max(maxWidth, line.length) }

class MainActivity : Activity() {

    private companion object {
        var hasClearedCommandUUIDForProcess = false
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val commandHandler = MazeRunnerCommandHandler()
    private var commandRunnerThread: Thread? = null

    private val apiKeyKey = "BUGSNAG_API_KEY"
    private val commandUUIDKey = "MAZE_COMMAND_UUID"
    lateinit var prefs: SharedPreferences

    var scenario: Scenario? = null
    var isActivityRecreate = false
    var mazeAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.isActivityRecreate = savedInstanceState != null
        log("MainActivity.onCreate called")
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        prefs = getPreferences(Context.MODE_PRIVATE)

        if (!hasClearedCommandUUIDForProcess) {
            clearStoredCommandUUID()
            hasClearedCommandUUIDForProcess = true
        }

        // Attempt to dismiss any system dialogs (such as "MazeRunner crashed")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            log("Broadcast ACTION_CLOSE_SYSTEM_DIALOGS intent")
            val closeDialog = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeDialog)
        }

        log("Set up clearUserData click handler")
        val clearUserData = findViewById<Button>(R.id.clearUserData)
        clearUserData.setOnClickListener {
            prefs.clearStoredApiKey(apiKeyKey)
            val apiKeyField = findViewById<EditText>(R.id.manualApiKey)
            apiKeyField.text.clear()
            log("Cleared user data")
        }

        if (prefs.contains(apiKeyKey)) {
            log("Using stored API key")
            val apiKey = prefs.getStoredApiKey(apiKeyKey)
            val apiKeyField = findViewById<EditText>(R.id.manualApiKey)
            apiKeyField.text.clear()
            apiKeyField.text.append(apiKey)
        }
        log("MainActivity.onCreate complete")
    }

    override fun onResume() {
        super.onResume()
        log("MainActivity.onResume called")

        // Don't start the command runner again if the activity is being recreated,
        // as it results in two threads executing commands concurrently and causing flakes.
        if (!this.isActivityRecreate) {
            startCommandRunner()
        }
        log("MainActivity.onResume complete")
    }

    private fun setMazeRunnerAddress() {
        mazeAddress = readMazeRunnerAddressFromConfig(timeout = false)
        if (!mazeAddress.isNullOrBlank()) {
            CiLog.info("Maze Runner address set from config file: $mazeAddress")
            return
        }

        // Assume we are running in legacy mode on BrowserStack
        if (mazeAddress.isNullOrBlank()) {
            CiLog.warn("Failed to read Maze Runner address from config file, defaulting to legacy BrowserStack address")
            mazeAddress = LEGACY_MAZE_ADDRESS
        }
    }

    private fun maybeRefreshMazeRunnerAddress() {
        if (mazeAddress != LEGACY_MAZE_ADDRESS) {
            return
        }

        val refreshedMazeAddress = readMazeRunnerAddressFromConfig(timeout = false)
        if (!refreshedMazeAddress.isNullOrBlank()) {
            mazeAddress = refreshedMazeAddress
            CiLog.info("Maze Runner address refreshed from config file: $mazeAddress")
        }
    }

    private fun readMazeRunnerAddressFromConfig(timeout: Boolean): String? {
        val context = applicationContext
        val externalFilesDir = context.getExternalFilesDir(null) ?: return null
        val configFile = File(externalFilesDir, "fixture_config.json")
        CiLog.info("Attempting to read Maze Runner address from ${configFile.path}")

        if (!timeout) {
            return readMazeRunnerAddressFromConfig(configFile)
        }

        // Poll for the fixture config file
        val pollEnd = System.currentTimeMillis() + CONFIG_FILE_TIMEOUT
        while (System.currentTimeMillis() < pollEnd) {
            val address = readMazeRunnerAddressFromConfig(configFile)
            if (!address.isNullOrBlank()) {
                return address
            }

            Thread.sleep(250)
        }

        return null
    }

    private fun setStoredCommandUUID(commandUUID: String) {
        with(prefs.edit()) {
            putString(commandUUIDKey, commandUUID)
            commit()
        }
        CiLog.info("lastCommandUUID set to: $commandUUID")
    }

    private fun clearStoredCommandUUID() {
        with(prefs.edit()) {
            remove(commandUUIDKey)
            commit()
        }
        CiLog.info("lastCommandUUID set to empty")
    }

    private fun getStoredCommandUUID(): String? {
        return prefs.getString(commandUUIDKey, "").orEmpty()
    }

    // Starts a thread to poll for Maze Runner actions to perform
    @Synchronized
    private fun startCommandRunner() {
        if (commandRunnerThread?.isAlive == true) {
            CiLog.info("Maze Runner command runner already active")
            return
        }

        val runner = thread(start = false) {
            try {
                if (mazeAddress == null) setMazeRunnerAddress()
                runCommandRunnerLoop()
            } finally {
                synchronized(this@MainActivity) {
                    if (commandRunnerThread === Thread.currentThread()) {
                        commandRunnerThread = null
                    }
                }
            }
        }

        commandRunnerThread = runner
        runner.start()
    }

    private fun runCommandRunnerLoop() {
        var polling = true
        while (polling) {
            Thread.sleep(1000)
            polling = fetchAndHandleNextCommand()
        }
    }

    private fun fetchAndHandleNextCommand(): Boolean {
        return try {
            maybeRefreshMazeRunnerAddress()

            val commandStr = readCommand()
            if (commandStr == "null") {
                CiLog.info("No Maze Runner commands queued")
                return true
            }

            CiLog.info("Received command: $commandStr")
            val mazeRunnerCommand = parseMazeRunnerCommand(commandStr)
            log("command.action: ${mazeRunnerCommand.action}")
            log("command.scenarioName: ${mazeRunnerCommand.scenarioName}")
            log("command.scenarioMode: ${mazeRunnerCommand.scenarioMode}")
            log("command.sessionsUrl: ${mazeRunnerCommand.sessionsUrl}")
            log("command.notifyUrl: ${mazeRunnerCommand.notifyUrl}")
            log("command.remoteConfigUrl: ${mazeRunnerCommand.remoteConfigUrl}")

            mainHandler.post { handleCommandOnUiThread(mazeRunnerCommand) }
            mazeRunnerCommand.action != "start_bugsnag" && mazeRunnerCommand.action != "run_scenario"
        } catch (e: Exception) {
            CiLog.error("Failed to fetch command from Maze Runner", e)
            true
        }
    }

    private fun handleCommandOnUiThread(command: MazeRunnerCommand) {
        try {
            // Display some feedback of the action being run on the UI
            val actionField = findViewById<EditText>(R.id.command_action)
            val scenarioField = findViewById<EditText>(R.id.command_scenario)
            actionField.setText(command.action)
            scenarioField.setText(command.scenarioName)
            commandHandler.handle(command)
        } catch (e: Exception) {
            CiLog.error("Failed to handle Maze Runner command", e)
        }
    }

    private fun readCommand(): String {
        val commandUrl = "http://$mazeAddress/command?after=${getStoredCommandUUID().orEmpty()}"
        CiLog.info("Requesting Maze Runner command from: $commandUrl")
        val urlConnection = URL(commandUrl).openConnection() as HttpURLConnection
        urlConnection.connectTimeout = MAZE_RUNNER_COMMAND_TIMEOUT_MS
        urlConnection.readTimeout = MAZE_RUNNER_COMMAND_TIMEOUT_MS
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
        notifyUrl: String,
        remoteConfigUrl: String
    ) {
        scenario = loadScenario(eventType, mode, sessionsUrl, notifyUrl, remoteConfigUrl)
        scenario?.startBugsnag(true)
    }

    // execute the pre-loaded scenario, or load it then execute it if needed
    private fun runScenario(
        eventType: String,
        mode: String,
        sessionsUrl: String,
        notifyUrl: String,
        remoteConfigUrl: String
    ) {
        if (scenario == null) {
            scenario = loadScenario(eventType, mode, sessionsUrl, notifyUrl, remoteConfigUrl)
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
        // Reset the command cursor so the next test starts fresh
        clearStoredCommandUUID()

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
        notifyUrl: String,
        remoteConfigUrl: String
    ): Scenario {
        val apiKeyField = findViewById<EditText>(R.id.manualApiKey)

        val manualMode = apiKeyField.text.isNotEmpty()
        val apiKey = when {
            manualMode -> apiKeyField.text.toString()
            else -> "a35a2a72bd230ac0aa0f52715bbdc6aa"
        }

        if (manualMode) {
            log("Running in manual mode with API key: $apiKey")
            prefs.setStoredApiKey(apiKeyKey, apiKey)
        }

        // send HTTP requests for intercepted log messages and metrics from Bugsnag.
        // reuse notify endpoint as we don't care about logs when running mazerunner in manual mode
        val mazerunnerHttpClient = MazerunnerHttpClient.fromEndpoint(notifyUrl)
        val effectiveRemoteConfigUrl = if (mode == "disable-remote-config") null else remoteConfigUrl

        val config = prepareConfig(apiKey, notifyUrl, sessionsUrl, effectiveRemoteConfigUrl, mazerunnerHttpClient) {
            val logMessage = it
            val interceptedLogMessages = scenario?.getInterceptedLogMessages()
            interceptedLogMessages?.any {
                Regex(it).matches(logMessage)
            } ?: false
        }
        return Scenario.load(this, config, eventType, mode, mazerunnerHttpClient)
    }

    private inner class MazeRunnerCommandHandler {
        fun handle(command: MazeRunnerCommand) {
            when (command.action) {
                "noop" -> {
                    CiLog.info("No Maze Runner command queuing, continuing to poll")
                }

                "start_bugsnag" -> {
                    setStoredCommandUUID(command.commandUUID)
                    startBugsnag(
                        command.scenarioName,
                        command.scenarioMode,
                        command.sessionsUrl,
                        command.notifyUrl,
                        command.remoteConfigUrl
                    )
                }

                "run_scenario" -> {
                    setStoredCommandUUID(command.commandUUID)
                    runScenario(
                        command.scenarioName,
                        command.scenarioMode,
                        command.sessionsUrl,
                        command.notifyUrl,
                        command.remoteConfigUrl
                    )
                }

                "clear_persistent_data" -> {
                    setStoredCommandUUID(command.commandUUID)
                    clearPersistentData()
                }

                "reset_uuid" -> clearStoredCommandUUID()
                else -> throw IllegalArgumentException("Unknown action: ${command.action}")
            }
        }
    }
}
