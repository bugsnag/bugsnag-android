package com.bugsnag.android.mazerunner

import android.os.Build
import android.util.Log
import com.bugsnag.android.BugsnagExitInfoPlugin
import com.bugsnag.android.Configuration
import com.bugsnag.android.Delivery
import com.bugsnag.android.DeliveryParams
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EndpointConfiguration
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Logger
import com.bugsnag.android.Session
import com.bugsnag.android.createDefaultDelivery

fun prepareConfig(
    apiKey: String,
    notify: String,
    sessions: String,
    mazerunnerHttpClient: MazerunnerHttpClient,
    logFilter: (msg: String) -> Boolean
): Configuration {
    val config = Configuration(apiKey)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        config.addPlugin(
            BugsnagExitInfoPlugin()
        )
    }

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

    config.logger = generateInterceptingLogger { logLevel, msg ->
        if (logFilter(msg)) {
            mazerunnerHttpClient.postLog(logLevel, msg)
        }
    }
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

/**
 * Sets a NOP implementation for the Session Tracking API, preventing delivery
 */
fun disableSessionDelivery(config: Configuration) {
    val baseDelivery = createDefaultDelivery()
    config.delivery = object : Delivery {
        override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
            return baseDelivery.deliver(payload, deliveryParams)
        }

        override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
            return DeliveryStatus.UNDELIVERED
        }
    }
}

/**
 * Sets a NOP implementation for the Error Tracking API, preventing delivery
 */
fun disableReportDelivery(config: Configuration) {
    val baseDelivery = createDefaultDelivery()
    config.delivery = object : Delivery {
        override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
            return DeliveryStatus.UNDELIVERED
        }

        override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
            return baseDelivery.deliver(payload, deliveryParams)
        }
    }
}

fun disableAllDelivery(config: Configuration) {
    config.delivery = object : Delivery {
        override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
            return DeliveryStatus.UNDELIVERED
        }

        override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
            return DeliveryStatus.UNDELIVERED
        }
    }
}
