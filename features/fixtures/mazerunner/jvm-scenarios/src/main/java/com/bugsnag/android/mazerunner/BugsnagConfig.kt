package com.bugsnag.android.mazerunner

import android.util.Log
import com.bugsnag.android.Configuration
import com.bugsnag.android.EndpointConfiguration
import com.bugsnag.android.Logger
import com.bugsnag.android.mazerunner.multiprocess.findCurrentProcessName
import java.net.URL

fun prepareConfig(
    apiKey: String,
    notify: String,
    sessions: String,
    logFilter: (msg: String) -> Boolean
): Configuration {
    val config = Configuration(apiKey)

    if (notify.isNotEmpty() && sessions.isNotEmpty()) {
        config.endpoints = EndpointConfiguration(notify, sessions)
    }

    // disable auto session tracking by default to avoid unnecessary requests in scenarios
    config.autoTrackSessions = false
    config.releaseStage = "mazerunner"

    with(config.enabledErrorTypes) {
        ndkCrashes = true
        anrs = true
    }

    // send HTTP requests for intercepted log messages from Bugsnag.
    // reuse notify endpoint as we don't care about logs when running mazerunner in manual mode
    val logEndpoint = URL(notify.replace("/notify", "/logs"))
    val mazerunnerHttpClient = MazerunnerHttpClient(logEndpoint)
    config.logger = generateInterceptingLogger { logLevel, msg ->
        if (logFilter(msg)) {
            mazerunnerHttpClient.postLog(logLevel, msg)
        }
    }
    config.addMetadata("process", "name", findCurrentProcessName())
    return config
}

private fun generateInterceptingLogger(
    cb: (logLevel: LogLevel, msg: String) -> Unit
) = object : Logger {
    private val TAG = "Bugsnag"

    override fun e(msg: String) {
        Log.e(TAG, msg)
        cb(LogLevel.ERROR, msg)
    }

    override fun e(msg: String, throwable: Throwable) {
        Log.e(TAG, msg, throwable)
        cb(LogLevel.ERROR, msg)
    }

    override fun w(msg: String) {
        Log.w(TAG, msg)
        cb(LogLevel.WARNING, msg)
    }

    override fun w(msg: String, throwable: Throwable) {
        Log.w(TAG, msg, throwable)
        cb(LogLevel.WARNING, msg)
    }

    override fun i(msg: String) {
        Log.i(TAG, msg)
        cb(LogLevel.INFO, msg)
    }

    override fun i(msg: String, throwable: Throwable) {
        Log.i(TAG, msg, throwable)
        cb(LogLevel.INFO, msg)
    }

    override fun d(msg: String) {
        Log.d(TAG, msg)
        cb(LogLevel.DEBUG, msg)
    }

    override fun d(msg: String, throwable: Throwable) {
        Log.d(TAG, msg, throwable)
        cb(LogLevel.DEBUG, msg)
    }
}
