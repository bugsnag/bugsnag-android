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
import com.bugsnag.android.mazerunner.CiLog
import com.bugsnag.android.mazerunner.LogLevel
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class RemoteConfigBasicScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    companion object {
        private const val TIMEOUT_SECONDS = 10L
        private const val UNHANDLED_DELAY_MS = 2000L
        private const val REMOTE_CONFIG_POLL_INTERVAL_MS = 100L
        private const val REMOTE_CONFIG_LOAD_DELAY_MS = 1000L

        private val REMOTE_CONFIG_MIN_FRESHNESS_MS =
            TimeUnit.SECONDS.toMillis(1)
    }

    private val handledEventSeen = AtomicBoolean(false)
    private val handledDeliveryCompleted = CountDownLatch(1)
    private val remoteConfigLoaded = CountDownLatch(1)

    init {
        config.addOnSend { event ->
            if (!event.isUnhandled && handledEventSeen.compareAndSet(false, true)) {
                mazerunnerHttpClient?.postLog(
                    LogLevel.INFO,
                    "RemoteConfigBasicScenario handled delivery completed"
                )

                handledDeliveryCompleted.countDown()
            }

            true
        }

        val baseDelivery = createDefaultDelivery()

        config.delivery = object : Delivery {
            override fun deliver(
                payload: EventPayload,
                deliveryParams: DeliveryParams
            ): DeliveryStatus {
                val status = baseDelivery.deliver(
                    payload,
                    deliveryParams
                )

                check(status == DeliveryStatus.DELIVERED) {
                    "Request failed, aborting scenario. status=$status"
                }

                return status
            }

            override fun deliver(
                payload: Session,
                deliveryParams: DeliveryParams
            ): DeliveryStatus {
                return baseDelivery.deliver(
                    payload,
                    deliveryParams
                )
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

        if (config.endpoints.configuration == null) {
            remoteConfigLoaded.countDown()
            return
        }

        Thread(
            {
                waitForFreshRemoteConfig()
                remoteConfigLoaded.countDown()
            },
            "remote-config-wait"
        ).start()
    }

    override fun startScenario() {
        super.startScenario()

        Thread(
            {
                try {
                    remoteConfigLoaded.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

                    Bugsnag.notify(
                        RuntimeException("Handled exception")
                    )

                    handledDeliveryCompleted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)

                    Thread.sleep(UNHANDLED_DELAY_MS)

                    throw IOException("Unhandled exception")
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            },
            "remote-config-scenario"
        ).start()
    }

    private fun waitForFreshRemoteConfig(
        timeoutMs: Long =
            TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)
    ): Boolean {
        val configFile = remoteConfigFile()

        if (configFile == null) {
            CiLog.error("RemoteConfigBasicScenario: Unable to determine config file")
            return false
        }

        CiLog.info("RemoteConfigBasicScenario: Waiting for config at ${configFile.absolutePath}")

        val deadline = System.currentTimeMillis() + timeoutMs

        while (System.currentTimeMillis() < deadline) {
            val expiry = readRemoteConfigExpiry(configFile)

            if (
                expiry != null &&
                expiry - System.currentTimeMillis() >
                    REMOTE_CONFIG_MIN_FRESHNESS_MS
            ) {
                CiLog.info("RemoteConfigBasicScenario: Fresh config found")
                Thread.sleep(REMOTE_CONFIG_LOAD_DELAY_MS)
                return true
            }

            Thread.sleep(REMOTE_CONFIG_POLL_INTERVAL_MS)
        }

        CiLog.warn("RemoteConfigBasicScenario: Timed out waiting for fresh config")
        return false
    }

    private fun remoteConfigFile(): File? {
        @Suppress("DEPRECATION")
        val versionCode = config.versionCode?.takeIf { it != 0 }
            ?: runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionCode }.getOrNull()
            ?: 0

        return File(
            File(context.cacheDir, "bugsnag/config"),
            "core-$versionCode.json"
        )
    }

    private fun readRemoteConfigExpiry(
        configFile: File
    ): Long? {
        if (!configFile.exists()) {
            return null
        }

        // Retry read once if it fails, to handle potential partial writes on slow CI disks
        repeat(2) {
            try {
                val text = configFile.readText()
                if (text.isNotEmpty()) {
                    val expiry = JSONObject(text).optString("configurationExpiry")

                    if (expiry.isNotBlank()) {
                        return remoteConfigExpiryFormat().parse(expiry)?.time
                    }
                }
            } catch (_: Exception) {
                Thread.sleep(100)
            }
        }
        return null
    }

    private fun remoteConfigExpiryFormat(): SimpleDateFormat {
        return SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}
