package com.bugsnag.android.internal

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.LockSupport

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
        if (samplingThresholdMillis > 0) ThreadSampler(watchedLooper.thread)
        else null

    @Volatile
    private var lastStackSampleTimestamp = 0L

    @Volatile
    private var lastHeartbeatTimestamp = 0L

    @Volatile
    private var lastHeartbeatPostedTimestamp = 0L

    @Volatile
    private var lastReportedHangTimestamp = 0L

    private val isRunning = AtomicBoolean(false)

    private var isAppHangDetected = false

    // the interval between heartbeats is half of the sampling interval, this is *not* how
    // frequently the heartbeats actually run since we don't heartbeat unless there seems to be a
    // problem
    private val heartbeatInterval =
        if (samplingThresholdMillis > 0) {
            samplingThresholdMillis / 2
        } else {
            appHangThresholdMillis / 2
        }

    private val heartbeat: Runnable = Heartbeat()

    private val isHeartbeatPending: Boolean
        get() = lastHeartbeatPostedTimestamp > 0L

    fun startMonitoring() {
        if (isRunning.compareAndSet(false, true)) {
            start()
        }
    }

    fun stopMonitoring() {
        if (isRunning.compareAndSet(true, false)) {
            handler.removeCallbacks(heartbeat)
            lastHeartbeatPostedTimestamp = 0L
            lastReportedHangTimestamp = 0L
            LockSupport.unpark(this)
        }
    }

    private fun postHeartbeat(now: Long): Boolean {
        if (!handler.post(heartbeat)) return false
        lastHeartbeatPostedTimestamp = now
        return true
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

    override fun run() {
        // Startup is treated as an implicit heartbeat since startMonitoring is called from main thread
        lastHeartbeatTimestamp = SystemClock.uptimeMillis()

        while (isRunning.get()) {
            val now = SystemClock.uptimeMillis()

            if (isHeartbeatPending && lastHeartbeatTimestamp >= lastHeartbeatPostedTimestamp) {
                onHeartbeatProcessed()
            }

            if (!isHeartbeatPending && now - lastHeartbeatTimestamp >= heartbeatInterval) {
                if (!postHeartbeat(now)) {
                    isRunning.set(false)
                    break
                }
            }

            parkWithTimeoutMs(calculateNextWaitTime(now))

            if (!isRunning.get()) {
                break
            }

            val currentTime = SystemClock.uptimeMillis()
            val currentTimeSinceEffectiveHeartbeat = timeSinceEffectiveHeartbeat(currentTime)

            if (shouldTakeSample(currentTime, currentTimeSinceEffectiveHeartbeat)) {
                threadSampler?.captureSample()
                lastStackSampleTimestamp = currentTime
            }

            if (currentTimeSinceEffectiveHeartbeat >= appHangThresholdMillis) {
                val hangReported = reportAppHang(currentTime, currentTimeSinceEffectiveHeartbeat)

                if (hangReported && appHangCooldownMillis > 0L) {
                    parkWithTimeoutMs(appHangCooldownMillis)
                }
            }
        }
    }

    private fun parkWithTimeoutMs(timeout: Long) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(timeout))
    }

    private fun calculateNextWaitTime(now: Long): Long {
        if (lastHeartbeatTimestamp <= 0L) {
            return heartbeatInterval
        }

        val timeSinceEffectiveHeartbeat = timeSinceEffectiveHeartbeat(now)
        if (timeSinceEffectiveHeartbeat >= appHangThresholdMillis) {
            return Long.MAX_VALUE
        }

        val timeToHang = appHangThresholdMillis - timeSinceEffectiveHeartbeat

        val timeToNextHeartbeat =
            if (!isHeartbeatPending) {
                maxOf(0L, heartbeatInterval - (now - lastHeartbeatTimestamp))
            } else {
                Long.MAX_VALUE
            }

        if (threadSampler == null) {
            return minOf(timeToHang, timeToNextHeartbeat)
        }

        val timeToNextSample = calculateTimeToNextStackSample(now, timeSinceEffectiveHeartbeat)
        return minOf(timeToNextSample, timeToHang, timeToNextHeartbeat)
    }

    private fun calculateTimeToNextStackSample(now: Long, timeSinceEffectiveHeartbeat: Long): Long {
        return if (lastStackSampleTimestamp > 0L) {
            maxOf(0L, samplingRateMillis - (now - lastStackSampleTimestamp))
        } else {
            maxOf(0L, samplingThresholdMillis - timeSinceEffectiveHeartbeat)
        }
    }

    private fun shouldTakeSample(now: Long, timeSinceEffectiveHeartbeat: Long): Boolean {
        if (threadSampler == null) return false
        if (timeSinceEffectiveHeartbeat < samplingThresholdMillis) return false

        val timeSinceLastSample =
            if (lastStackSampleTimestamp > 0L) {
                now - lastStackSampleTimestamp
            } else {
                Long.MAX_VALUE
            }

        return timeSinceLastSample >= samplingRateMillis
    }

    private fun timeSinceEffectiveHeartbeat(now: Long): Long {
        val effectiveTimestamp =
            if (isHeartbeatPending) lastHeartbeatPostedTimestamp
            else lastHeartbeatTimestamp
        return now - effectiveTimestamp
    }

    private fun onHeartbeatProcessed() {
        lastHeartbeatPostedTimestamp = 0L
        isAppHangDetected = false

        threadSampler?.resetSampling()
        lastStackSampleTimestamp = 0L
    }

    private inner class Heartbeat : Runnable {
        override fun run() {
            lastHeartbeatTimestamp = SystemClock.uptimeMillis()
        }

        override fun toString(): String {
            return "Bugsnag AppHang Heartbeat"
        }
    }
}
