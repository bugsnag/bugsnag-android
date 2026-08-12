package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.Delivery
import com.bugsnag.android.DeliveryParams
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EndpointConfiguration
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Session
import com.bugsnag.android.createDefaultDelivery
import com.bugsnag.android.mazerunner.LogLevel
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class RemoteConfigBasicScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {
    companion object {
        private const val UNHANDLED_DELAY_MS = 5000L
        private const val TIMEOUT_SECONDS = 10L
        private const val REMOTE_CONFIG_POLL_INTERVAL_MS = 100L
    }

    private val handledErrorDelivered = AtomicBoolean(false)
    private val handledDeliveryCompleted = CountDownLatch(1)

    init {
        val baseDelivery = createDefaultDelivery()
        config.delivery = object : Delivery {
            override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                val status = baseDelivery.deliver(payload, deliveryParams)
                check(status == DeliveryStatus.DELIVERED) {
                    "Request failed, aborting scenario. status=$status"
                }

                if (payload.event?.isUnhandled == false && handledErrorDelivered.compareAndSet(false, true)) {
                    // Trigger the crash only after the handled delivery has completed, then give
                    // the event a short window to flush before the app dies.
                    mazerunnerHttpClient?.postLog(
                        LogLevel.INFO,
                        "RemoteConfigBasicScenario handled delivery completed"
                    )
                    handledDeliveryCompleted.countDown()
                }

                return status
            }

            override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
                return baseDelivery.deliver(payload, deliveryParams)
            }
        }

        if (eventMetadata == "disable-remote-config") {
            config.endpoints = EndpointConfiguration(
                config.endpoints.notify,
                config.endpoints.sessions,
                null
            )
        }
    }

    override fun startBugsnag(startBugsnagOnly: Boolean) {
        super.startBugsnag(startBugsnagOnly)

        if (config.endpoints.configuration != null) {
            waitForFreshRemoteConfig()
        }
    }

    override fun startScenario() {
        super.startScenario()
        Thread {
            try {
                handledDeliveryCompleted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                Thread.sleep(UNHANDLED_DELAY_MS)
                throw IOException("Unhandled exception")
            } catch (ex: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }.start()
        Bugsnag.notify(RuntimeException("Handled exception"))
    }

    private fun waitForFreshRemoteConfig(timeoutMs: Long = TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)) {
        val configFile = remoteConfigFile() ?: return
        val deadline = System.currentTimeMillis() + timeoutMs

        while (System.currentTimeMillis() < deadline) {
            val expiry = readRemoteConfigExpiry(configFile)
            if (expiry == null) {
                Thread.sleep(REMOTE_CONFIG_POLL_INTERVAL_MS)
                continue
            }

            if (expiry > System.currentTimeMillis()) {
                return
            }

            Thread.sleep(REMOTE_CONFIG_POLL_INTERVAL_MS)
        }
    }

    private fun remoteConfigFile(): File? {
        val versionCode = config.versionCode ?: return null
        return File(File(context.cacheDir, "bugsnag/config"), "core-$versionCode.json")
    }

    private fun readRemoteConfigExpiry(configFile: File): Long? {
        if (!configFile.exists()) {
            return null
        }

        return try {
            val expiry = JSONObject(configFile.readText()).optString("configurationExpiry")
            if (expiry.isBlank()) {
                null
            } else {
                java.time.Instant.parse(expiry).toEpochMilli()
            }
        } catch (_: Exception) {
            null
        }
    }
}
