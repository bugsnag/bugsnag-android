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

    private fun effectiveHeartbeatTimestamp(): Long {
        val postedTimestamp = lastHeartbeatPostedTimestamp
        return if (postedTimestamp > 0L) postedTimestamp else lastHeartbeatTimestamp
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
            val effectiveTimestamp = effectiveHeartbeatTimestamp()
            val timeSinceEffectiveHeartbeat = now - effectiveTimestamp

            // Post heartbeat at fixed intervals (if one isn't already pending)
            if (lastHeartbeatPostedTimestamp == 0L) {
                val timeSinceHeartbeat = now - lastHeartbeatTimestamp
                if (timeSinceHeartbeat >= heartbeatInterval) {
                    if (handler.post(heartbeat)) {
                        lastHeartbeatPostedTimestamp = now
                    } else {
                        isRunning.set(false)
                        break
                    }
                }
            }

            val waitMillis = calculateNextWaitTime(now, timeSinceEffectiveHeartbeat)
            parkWithTimeoutMs(waitMillis)

            if (!isRunning.get()) break

            val currentTime = SystemClock.uptimeMillis()
            val currentTimeSinceEffectiveHeartbeat = currentTime - effectiveHeartbeatTimestamp()

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

    private fun calculateNextWaitTime(now: Long, timeSinceEffectiveHeartbeat: Long): Long {
        if (lastHeartbeatTimestamp <= 0L) return heartbeatInterval
        if (timeSinceEffectiveHeartbeat >= appHangThresholdMillis) return Long.MAX_VALUE

        val timeToHang = appHangThresholdMillis - timeSinceEffectiveHeartbeat

        // If no heartbeat is pending, also consider time until next heartbeat should be posted
        val timeToNextHeartbeat = if (lastHeartbeatPostedTimestamp == 0L) {
            val timeSinceHeartbeat = now - lastHeartbeatTimestamp
            maxOf(0L, heartbeatInterval - timeSinceHeartbeat)
        } else {
            Long.MAX_VALUE
        }

        if (threadSampler == null) return minOf(timeToHang, timeToNextHeartbeat)

        val timeToNextSample = calculateTimeToNextStackSample(now, timeSinceEffectiveHeartbeat)
        return minOf(timeToNextSample, timeToHang, timeToNextHeartbeat)
    }

    private fun calculateTimeToNextStackSample(
        now: Long,
        timeSinceEffectiveHeartbeat: Long
    ): Long {
        return if (lastStackSampleTimestamp > 0L) {
            val timeToNextSample = samplingRateMillis - (now - lastStackSampleTimestamp)
            maxOf(0L, timeToNextSample)
        } else {
            maxOf(0L, samplingThresholdMillis - timeSinceEffectiveHeartbeat)
        }
    }

    private fun shouldTakeSample(currentTime: Long, timeSinceHeartbeat: Long): Boolean {
        if (threadSampler == null) return false
        if (timeSinceHeartbeat < samplingThresholdMillis) return false

        val timeSinceLastSample = if (lastStackSampleTimestamp <= 0L) {
            Long.MAX_VALUE
        } else {
            currentTime - lastStackSampleTimestamp
        }

        return timeSinceLastSample >= samplingRateMillis
    }

    internal fun onHeartbeat() {
        lastHeartbeatTimestamp = SystemClock.uptimeMillis()
        lastHeartbeatPostedTimestamp = 0L
        isAppHangDetected = false

        threadSampler?.resetSampling()
        lastStackSampleTimestamp = 0L
    }

    private inner class Heartbeat : Runnable {
        override fun run() = onHeartbeat()
        override fun toString(): String = "Bugsnag AppHang Heartbeat"
    }
}
