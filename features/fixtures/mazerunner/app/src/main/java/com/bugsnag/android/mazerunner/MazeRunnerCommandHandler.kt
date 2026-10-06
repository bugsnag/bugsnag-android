package com.bugsnag.android.mazerunner

interface CommandExecutor {
    fun startBugsnag(
        eventType: String,
        mode: String,
        sessionsUrl: String,
        notifyUrl: String,
        remoteConfigUrl: String
    )
    fun runScenario(
        eventType: String,
        mode: String,
        sessionsUrl: String,
        notifyUrl: String,
        remoteConfigUrl: String
    )
    fun clearPersistentData()
    fun setStoredCommandUUID(commandUUID: String)
    fun clearStoredCommandUUID()
}

class MazeRunnerCommandHandler(private val executor: CommandExecutor) {
    fun handle(command: MazeRunnerCommand) {
        when (command.action) {
            "noop" -> {
                CiLog.info("No Maze Runner command queuing, continuing to poll")
            }

            "start_bugsnag" -> {
                executor.setStoredCommandUUID(command.commandUUID)
                executor.startBugsnag(
                    command.scenarioName,
                    command.scenarioMode,
                    command.sessionsUrl,
                    command.notifyUrl,
                    command.remoteConfigUrl
                )
            }

            "run_scenario" -> {
                executor.setStoredCommandUUID(command.commandUUID)
                executor.runScenario(
                    command.scenarioName,
                    command.scenarioMode,
                    command.sessionsUrl,
                    command.notifyUrl,
                    command.remoteConfigUrl
                )
            }

            "clear_persistent_data" -> {
                executor.setStoredCommandUUID(command.commandUUID)
                executor.clearPersistentData()
            }

            "reset_uuid" -> executor.clearStoredCommandUUID()
            else -> throw IllegalArgumentException("Unknown action: ${command.action}")
        }
    }
}
