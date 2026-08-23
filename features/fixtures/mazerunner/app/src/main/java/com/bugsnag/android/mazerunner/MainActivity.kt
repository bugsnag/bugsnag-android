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
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

private const val MAZE_RUNNER_COMMAND_TIMEOUT_MS = 5000
private const val LEGACY_MAZE_ADDRESS = "bs-local.com:9339"

class MainActivity : Activity(), CommandExecutor {

    private companion object {
        var hasClearedCommandUUIDForProcess = false
        var activeCommandRunnerThread: Thread? = null
    }

    private val activityInstanceId = Integer.toHexString(System.identityHashCode(this))
    private val mainHandler = Handler(Looper.getMainLooper())
    private val commandHandler = MazeRunnerCommandHandler(this)

    private val apiKeyKey = "BUGSNAG_API_KEY"
    private val commandUUIDKey = "MAZE_COMMAND_UUID"

    var scenario: Scenario? = null
    var isActivityRecreate = false
    var mazeAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.isActivityRecreate = savedInstanceState != null
        log("MainActivity.onCreate called: activity=$activityInstanceId")
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("mazerunner", Context.MODE_PRIVATE)

        if (!hasClearedCommandUUIDForProcess) {
            CiLog.info("First onCreate for this process, last command UUID: '${getStoredCommandUUID()}'")
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

        startCommandRunner()
        log("MainActivity.onResume complete")
    }

    override fun onDestroy() {
        CiLog.info("MainActivity.onDestroy called: activity=$activityInstanceId")
        stopCommandRunner()
        super.onDestroy()
    }

    private fun setMazeRunnerAddress() {
        var address = MazeRunnerAddressReader.readFromConfig(applicationContext, timeout = false)
        if (address == "local:9339") {
            address = "bs-local.com:9339"
        }

        if (!address.isNullOrBlank()) {
            mazeAddress = address
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

        var refreshedMazeAddress = MazeRunnerAddressReader.readFromConfig(applicationContext, timeout = false)
        if (refreshedMazeAddress == "local:9339") {
            refreshedMazeAddress = "bs-local.com:9339"
        }

        if (!refreshedMazeAddress.isNullOrBlank()) {
            mazeAddress = refreshedMazeAddress
            CiLog.info("Maze Runner address refreshed from config file: $mazeAddress")
        }
    }

    override fun setStoredCommandUUID(commandUUID: String) {
        getSharedPreferences("mazerunner", Context.MODE_PRIVATE).edit().putString(commandUUIDKey, commandUUID).commit()
        CiLog.info("lastCommandUUID set to: $commandUUID (activity=$activityInstanceId)")
    }

    override fun clearStoredCommandUUID() {
        getSharedPreferences("mazerunner", Context.MODE_PRIVATE).edit().remove(commandUUIDKey).commit()
        CiLog.info("lastCommandUUID cleared (activity=$activityInstanceId)")
    }

    private fun getStoredCommandUUID(): String {
        return getSharedPreferences("mazerunner", Context.MODE_PRIVATE).getString(commandUUIDKey, "").orEmpty()
    }

    // Starts a thread to poll for Maze Runner actions to perform
    @Synchronized
    private fun startCommandRunner() {
        if (activeCommandRunnerThread?.isAlive == true) {
            CiLog.info("Maze Runner command runner already active (current activity=$activityInstanceId)")
            return
        }

        CiLog.info(
            "Starting command runner: " +
                "activity=$activityInstanceId, " +
                "thread=${Thread.currentThread().name}, " +
                "uuid='${getStoredCommandUUID()}'"
        )

        val runner = thread(
            start = false,
            name = "maze-command-$activityInstanceId"
        ) {
            try {
                CiLog.info(
                    "Command runner thread started: " +
                        "activity=$activityInstanceId, " +
                        "thread=${Thread.currentThread().name}"
                )

                if (mazeAddress == null) setMazeRunnerAddress()
                runCommandRunnerLoop()
            } finally {
                CiLog.info(
                    "Command runner thread finished: " +
                        "activity=$activityInstanceId, " +
                        "thread=${Thread.currentThread().name}"
                )

                synchronized(MainActivity::class.java) {
                    if (activeCommandRunnerThread === Thread.currentThread()) {
                        activeCommandRunnerThread = null
                    }
                }
            }
        }

        activeCommandRunnerThread = runner
        runner.start()
    }

    private fun stopCommandRunner() {
        val runner = synchronized(MainActivity::class.java) {
            val r = activeCommandRunnerThread
            activeCommandRunnerThread = null
            r
        }

        if (runner?.isAlive == true) {
            CiLog.info(
                "Stopping command runner: " +
                    "activity=$activityInstanceId, " +
                    "thread=${runner.name}"
            )
            runner.interrupt()
        }
    }

    private fun runCommandRunnerLoop() {
        var polling = true

        while (polling && !Thread.currentThread().isInterrupted) {
            try {
                Thread.sleep(1000)
                polling = fetchAndHandleNextCommand()
            } catch (interrupted: InterruptedException) {
                CiLog.info(
                    "Command runner interrupted: " +
                        "activity=$activityInstanceId"
                )
                Thread.currentThread().interrupt()
                polling = false
            }
        }

        CiLog.info("Maze Runner command runner stopped")
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
        val storedUUID = getStoredCommandUUID()
        val commandUrl = "http://$mazeAddress/command?after=$storedUUID"

        CiLog.info(
            "Requesting Maze Runner command: " +
                "activity=$activityInstanceId, " +
                "thread=${Thread.currentThread().name}, " +
                "uuid='$storedUUID', " +
                "url=$commandUrl"
        )

        val urlConnection = URL(commandUrl).openConnection() as HttpURLConnection
        urlConnection.connectTimeout = MAZE_RUNNER_COMMAND_TIMEOUT_MS
        urlConnection.readTimeout = MAZE_RUNNER_COMMAND_TIMEOUT_MS
        try {
            val responseCode = urlConnection.responseCode
            if (responseCode == 200) {
                return urlConnection.inputStream.use { it.reader().readText() }
            }

            if (responseCode == 400) {
                val rejectedUuid = getStoredCommandUUID()
                CiLog.warn("Maze Runner returned 400 Bad Request for command UUID: $rejectedUuid")

                clearStoredCommandUUID()
                CiLog.info("Command UUID after clearing: '${getStoredCommandUUID()}'")
            }

            val errorMessage = urlConnection.errorStream?.use { it.reader().readText() }.orEmpty()
            CiLog.error(
                "Failed to GET $commandUrl (HTTP $responseCode " +
                    "${urlConnection.responseMessage}):\n" +
                    "${"-".repeat(errorMessage.width.coerceAtLeast(1))}\n" +
                    "$errorMessage\n" +
                    "-".repeat(errorMessage.width.coerceAtLeast(1))
            )
            throw IOException("Failed to GET $commandUrl (HTTP $responseCode)")
        } catch (ioe: IOException) {
            CiLog.error("Read of Maze Runner command failed", ioe)
            throw ioe
        }
    }

    // load the scenario first, which initialises bugsnag without running any crashy code
    override fun startBugsnag(
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
    override fun runScenario(
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
    override fun clearPersistentData() {
        CiLog.info("Clearing persistent data")
        scenario = null
        PersistentData(applicationContext).clear()
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

        val prefs = getSharedPreferences("mazerunner", Context.MODE_PRIVATE)
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
}
