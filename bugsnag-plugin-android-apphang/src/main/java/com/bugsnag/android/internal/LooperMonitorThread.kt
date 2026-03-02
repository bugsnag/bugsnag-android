package com.bugsnag.android.internal

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

internal class LooperMonitorThread(
    watchedLooper: Looper,
    private val appHangThresholdMillis: Long,
    private val appHangCooldownMillis: Long,
    private val samplingThresholdMillis: Long,
    private val samplingRateMillis: Long,
    private val onAppHangDetected: (timeSinceLastHeartbeat: Long, ThreadSampler?) -> Unit
) : Thread("Bugsnag AppHang Monitor: ${watchedLooper.thread.name}") {
    private val handler: Handler = Handler(watchedLooper)

    private val threadSampler: ThreadSampler? =
        if (isSamplingEnabled) ThreadSampler(watchedLooper.thread)
        else null

    private val heartbeatInterval =
        if (isSamplingEnabled) samplingThresholdMillis / 2
        else appHangThresholdMillis / 2

    @Volatile
    private var lastStackSampleTimestamp = 0L

    @Volatile
    private var lastHeartbeatTimestamp = 0L

    @Volatile
    private var lastReportedHangTimestamp = 0L

    @Volatile
    private var isAppHangDetected = false

    private var lastHeartbeatPostTimestamp = 0L

    private val isRunning = AtomicBoolean(false)

    private val heartbeat: Runnable = Heartbeat()

    private val isSamplingEnabled: Boolean
        get() = samplingThresholdMillis > 0

    /**
     * A heartbeat is pending iff the timestamp we posted the latest one at is after the last
     * heartbeat timestamp.
     */
    private val isHeartbeatPending
        get() = lastHeartbeatPostTimestamp > lastHeartbeatTimestamp

    fun startMonitoring() {
        if (isRunning.compareAndSet(false, true)) {
            start()
        }
    }

    fun stopMonitoring() {
        if (isRunning.compareAndSet(true, false)) {
            handler.removeCallbacks(heartbeat)
            lastReportedHangTimestamp = 0L
            interrupt()
        }
    }

    private fun reportAppHang(currentTime: Long, timeSinceLastHeartbeat: Long): Boolean {
        if (isAppHangDetected) {
            return false
        }

        if (appHangCooldownMillis > 0L && lastReportedHangTimestamp > 0L) {
            val timeSinceLastReport = currentTime - lastReportedHangTimestamp
            if (timeSinceLastReport < appHangCooldownMillis) {
                return false
            }
        }

        isAppHangDetected = true
        lastReportedHangTimestamp = currentTime
        onAppHangDetected(timeSinceLastHeartbeat, threadSampler)
        return true
    }

    /**
     * Calculate how long (in millis) the monitor thread should go to sleep for. The exact value
     * depends on what the next expected event is:
     *
     * - normal operation: (time to sample start || time to app hang) / 2
     * - sampling: sampling interval time
     * - app hang: app cooldown time
     */
    private fun timeUntilNextWakeup(): Long {
        if (isAppHangDetected && appHangCooldownMillis > 0) {
            return appHangCooldownMillis
        }

        if (isSamplingEnabled) {
            val now = currentTime()
            val timeSinceHeartbeat = now - lastHeartbeatTimestamp

            if (timeSinceHeartbeat > samplingThresholdMillis) {
                return samplingRateMillis
            }
        }

        return heartbeatInterval
    }

    override fun run() {
        lastHeartbeatTimestamp = currentTime()
        while (isRunning.get()) {
            if (!postHeartbeat()) {
                break
            }

            waitForMillis(timeUntilNextWakeup())

            // early exit upon termination
            if (!isRunning.get()) {
                break
            }

            val now = currentTime()
            val timeSinceHeartbeat = now - lastHeartbeatTimestamp

            if (isHeartbeatPending) {
                if (shouldTakeSample(now)) {
                    threadSampler?.captureSample()
                    lastStackSampleTimestamp = now
                }

                if (timeSinceHeartbeat >= appHangThresholdMillis) {
                    reportAppHang(now, timeSinceHeartbeat)
                }
            } else {
                isAppHangDetected = false
                threadSampler?.resetSampling()
                lastStackSampleTimestamp = 0L
            }
        }
    }

    /**
     * Post the heartbeat message to the monitored queue, returning `true` if the monitoring
     * loop should continue or `false` if we should stop attempting to monitor the thread
     */
    private fun postHeartbeat(): Boolean {
        if (!isRunning.get()) {
            return false
        } else if (isHeartbeatPending) {
            return true
        }

        // we track the "post" timestamp before sending it so that the "post" is always <= heartbeat
        lastHeartbeatPostTimestamp = currentTime()

        if (!handler.post(heartbeat)) {
            isRunning.set(false)
            return false
        }

        return true
    }

    private fun waitForMillis(timeout: Long) {
        val endTime = currentTime() + timeout
        while (isRunning.get()) {
            val remainingTime = endTime - currentTime()
            if (remainingTime <= 0) {
                break
            }

            try {
                sleep(remainingTime)
            } catch (_: InterruptedException) {
            }
        }
    }

    private fun shouldTakeSample(now: Long): Boolean {
        if (threadSampler == null) {
            return false
        }

        val timeSinceHeartbeat = now - lastHeartbeatTimestamp
        if (timeSinceHeartbeat < samplingThresholdMillis) {
            return false
        }

        val timeSinceLastSample =
            if (lastStackSampleTimestamp <= 0L) {
                Long.MAX_VALUE
            } else {
                now - lastStackSampleTimestamp
            }

        return timeSinceLastSample >= samplingRateMillis
    }

    internal fun currentTime(): Long = SystemClock.uptimeMillis()

    private inner class Heartbeat : Runnable {
        override fun run() {
            lastHeartbeatTimestamp = currentTime()
        }

        override fun toString(): String {
            return "Bugsnag AppHang Heartbeat"
        }
    }
}
