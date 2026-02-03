package com.bugsnag.android.internal

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.LockSupport
import kotlin.compareTo
import kotlin.text.compareTo
import kotlin.text.get
import kotlin.text.set

internal class LooperMonitorThread(
    watchedLooper: Looper,
    private val appHangThresholdMillis: Long,
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

    private val isRunning = AtomicBoolean(false)

    private var isAppHangDetected = false

    private val heartbeat: Runnable = Heartbeat()

    fun startMonitoring() {
        if (isRunning.compareAndSet(false, true)) {
            start()
        }
    }

    fun stopMonitoring() {
        if (isRunning.compareAndSet(true, false)) {
            handler.removeCallbacks(heartbeat)
            LockSupport.unpark(this)
        }
    }

    internal fun resetHeartbeatTimer() {
        LockSupport.unpark(this)
    }

    private fun reportAppHang(timeSinceLastHeartbeat: Long) {
        if (isAppHangDetected) {
            return
        }

        isAppHangDetected = true
        onAppHangDetected(timeSinceLastHeartbeat, threadSampler)
    }

    override fun run() {
        handler.post(heartbeat)

        while (isRunning.get()) {
            val now = SystemClock.uptimeMillis()
            val timeSinceHeartbeat = now - lastHeartbeatTimestamp

            // Wait until next sample time or hang detection time, whichever comes first
            val waitMillis = calculateNextWaitTime(now, timeSinceHeartbeat)
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(waitMillis))

            if (!isRunning.get()) break

            val currentTime = SystemClock.uptimeMillis()
            val currentTimeSinceHeartbeat = currentTime - lastHeartbeatTimestamp

            if (shouldTakeSample(currentTime, currentTimeSinceHeartbeat)) {
                threadSampler?.captureSample()
                lastStackSampleTimestamp = currentTime
            }

            if (currentTimeSinceHeartbeat >= appHangThresholdMillis) {
                reportAppHang(currentTimeSinceHeartbeat)
            }

            if (!handler.post(heartbeat)) {
                isRunning.set(false)
            }
        }
    }

    private fun calculateNextWaitTime(now: Long, timeSinceHeartbeat: Long): Long {
        if (lastHeartbeatTimestamp <= 0L) return appHangThresholdMillis
        if (timeSinceHeartbeat >= appHangThresholdMillis) return Long.MAX_VALUE

        val timeToHang = appHangThresholdMillis - timeSinceHeartbeat
        if (threadSampler == null) return timeToHang

        return calculateTimeToNextStackSample(now, timeToHang, timeSinceHeartbeat)
    }

    private fun calculateTimeToNextStackSample(
        now: Long,
        timeToHang: Long,
        timeSinceHeartbeat: Long
    ): Long {
        return if (lastStackSampleTimestamp > 0L) {
            // Already sampling - wait for next sample
            val timeToNextSample = samplingRateMillis - (now - lastStackSampleTimestamp)
            minOf(timeToNextSample, timeToHang)
        } else {
            val timeToSamplingStart = samplingThresholdMillis - timeSinceHeartbeat
            minOf(timeToSamplingStart, timeToHang)
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

    private inner class Heartbeat : Runnable {
        override fun run() {
            lastHeartbeatTimestamp = SystemClock.uptimeMillis()
            isAppHangDetected = false

            // Reset sampler when thread recovers
            threadSampler?.resetSampling()
            lastStackSampleTimestamp = 0L

            resetHeartbeatTimer()
        }

        override fun toString(): String {
            return "Bugsnag AppHang Heartbeat"
        }
    }
}
