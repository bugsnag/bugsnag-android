package com.bugsnag.android.mazerunner

import com.bugsnag.android.Configuration
import com.bugsnag.android.EndpointConfiguration
import com.bugsnag.android.mazerunner.multiprocess.findCurrentProcessName

fun prepareConfig(
    apiKey: String,
    notify: String,
    sessions: String
): Configuration {
    val config = Configuration(apiKey)

    if (notify.isNotEmpty() && sessions.isNotEmpty()) {
        config.endpoints = EndpointConfiguration(notify, sessions)
    }

    with(config.enabledErrorTypes) {
        ndkCrashes = true
        anrs = true
    }
    config.logger = getBugsnagLogger()
    config.addMetadata("process", "name", findCurrentProcessName())
    return config
}
