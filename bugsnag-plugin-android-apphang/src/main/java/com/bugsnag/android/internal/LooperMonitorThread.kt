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
    private val onAppHangDetected: (timeSinceLastHeartbeat: Long) -> Unit
) : Thread("Bugsnag AppHang Monitor: ${watchedLooper.thread.name}") {
    private val handler: Handler = Handler(watchedLooper)

    private var lastHeartbeatTimestamp = 0L

    private val isRunning = AtomicBoolean(false)

    private var isAppHangDetected = false

    private val heartbeat: Runnable = Heartbeat()

    private fun calculateTimeToAppHang(now: Long): Long =
        (lastHeartbeatTimestamp + appHangThresholdMillis) - now

    fun startMonitoring() {
        if (isRunning.compareAndSet(false, true)) {
            start()
        }
    }

    fun stopMonitoring() {
        if (isRunning.compareAndSet(true, false)) {
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
        onAppHangDetected(timeSinceLastHeartbeat)
    }

    override fun run() {
        handler.post(heartbeat)

        while (isRunning.get()) {
            val waitThreshold =
                if (lastHeartbeatTimestamp <= 0L) appHangThresholdMillis
                else calculateTimeToAppHang(SystemClock.uptimeMillis())

            val waitThresholdNanos = TimeUnit.MILLISECONDS.toNanos(waitThreshold)
            LockSupport.parkNanos(waitThresholdNanos)

            if (!isRunning.get()) break

            val timeSinceLastHeartbeat = SystemClock.uptimeMillis() - lastHeartbeatTimestamp

            if (timeSinceLastHeartbeat >= appHangThresholdMillis) {
                reportAppHang(timeSinceLastHeartbeat)
            } else {
                handler.post(heartbeat)
            }
        }
    }

    private inner class Heartbeat : Runnable {
        override fun run() {
            lastHeartbeatTimestamp = SystemClock.uptimeMillis()
            isAppHangDetected = false

            resetHeartbeatTimer()
        }

        override fun toString(): String {
            return "Bugsnag AppHang Heartbeat"
        }
    }
}
