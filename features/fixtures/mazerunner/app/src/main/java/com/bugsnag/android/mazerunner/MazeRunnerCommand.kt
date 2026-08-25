package com.bugsnag.android.mazerunner

import org.json.JSONObject

data class MazeRunnerCommand(
    val action: String,
    val scenarioName: String,
    val scenarioMode: String,
    val sessionsUrl: String,
    val notifyUrl: String,
    val remoteConfigUrl: String,
    val commandUUID: String
)

fun parseMazeRunnerCommand(commandStr: String) = MazeRunnerCommand(
    action = JSONObject(commandStr).optString("action"),
    scenarioName = JSONObject(commandStr).optString("scenario_name"),
    scenarioMode = JSONObject(commandStr).optString("scenario_mode"),
    sessionsUrl = JSONObject(commandStr).optString("sessions_endpoint"),
    notifyUrl = JSONObject(commandStr).optString("notify_endpoint"),
    remoteConfigUrl = JSONObject(commandStr).optString("error_config_endpoint"),
    commandUUID = JSONObject(commandStr).optString("uuid")
)
