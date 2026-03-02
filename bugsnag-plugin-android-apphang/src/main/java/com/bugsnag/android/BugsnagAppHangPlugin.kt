package com.bugsnag.android

import androidx.annotation.VisibleForTesting
import com.bugsnag.android.internal.LooperMonitorThread
import com.bugsnag.android.internal.ThreadSampler

/**
 * An alternative to Application Not Responding (ANR) reporting with configurable timeouts.
 */
class BugsnagAppHangPlugin @JvmOverloads constructor(
    configuration: AppHangConfiguration = AppHangConfiguration()
) : Plugin {
    private val appHangThresholdMillis = configuration.appHangThresholdMillis
    private val samplingThresholdMillis = configuration.stackSamplingThresholdMillis ?: 0
    private val samplingRateMillis = configuration.stackSamplingIntervalMillis
    private val appHangCooldownMillis = configuration.appHangCooldownMillis
    private val watchedLooper = configuration.watchedLooper

    private var client: Client? = null
    private var monitorThread: LooperMonitorThread? = null

    override fun load(client: Client) {
        this.client = client
        this.client?.sessionTracker?.addObserver { stateEvent ->
            if (stateEvent is StateEvent.UpdateInForeground) {
                if (stateEvent.inForeground) {
                    startMonitoring()
                } else {
                    stopMonitoring()
                }
            }
        }

        if (client.sessionTracker?.isInForeground == true) {
            startMonitoring()
        }
    }

    override fun unload() {
        stopMonitoring()
        client = null
    }

    private fun reportAppHang(timeSinceLastHeartbeat: Long, sampler: ThreadSampler?) {
        val watchedThread = watchedLooper.thread
        val stackTrace = watchedThread.stackTrace
        val threadName = watchedThread.name

        client?.notify(
            AppHangException(
                "$threadName has not responded in ${timeSinceLastHeartbeat}ms",
                stackTrace
            )
        ) { event ->
            event.errors.firstOrNull()?.errorClass = "AppHang"
            sampler?.createError(event)

            @Suppress("DEPRECATION")
            event.setErrorReportingThread(watchedThread.id)
            true
        }
    }

    @VisibleForTesting
    internal fun startMonitoring() {
        if (monitorThread != null) {
            return
        }

        monitorThread = LooperMonitorThread(
            watchedLooper,
            appHangThresholdMillis,
            appHangCooldownMillis,
            if (samplingThresholdMillis in 1..appHangThresholdMillis) samplingThresholdMillis else 0,
            if (samplingRateMillis in 1..appHangThresholdMillis) samplingRateMillis else 0,
            this::reportAppHang
        )

        monitorThread?.startMonitoring()
    }

    @VisibleForTesting
    internal fun stopMonitoring() {
        monitorThread?.stopMonitoring()
        monitorThread = null
    }
}
