package com.bugsnag.android.internal

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

internal class LooperMonitorThread(
    watchedLooper: Looper,
    private val appHangThresholdMillis: Long,
    private val onAppHangDetected: (timeSinceLastHeartbeat: Long) -> Unit
) : Thread("Bugsnag AppHang Monitor: ${watchedLooper.thread.name}") {
    private val handler: Handler = Handler(watchedLooper)

    private var lastHeartbeatTimestamp = 0L

    private val isRunning = AtomicBoolean(false)

    private var isAppHangDetected = false

    private val heartbeatLock = ReentrantLock(false)
    private val heartbeatCondition = heartbeatLock.newCondition()

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
            interrupt()
        }
    }

    internal fun resetHeartbeatTimer() {
        heartbeatLock.lock()
        try {
            heartbeatCondition.signalAll()
        } finally {
            heartbeatLock.unlock()
        }
    }

    private fun reportAppHang(timeSinceLastHeartbeat: Long) {
        if (isAppHangDetected) {
            // avoid reporting duplicate AppHangs
            return
        }

        isAppHangDetected = true
        onAppHangDetected(timeSinceLastHeartbeat)
    }

    override fun run() {
        handler.post(heartbeat)

        while (isRunning.get()) {
            heartbeatLock.lock()
            try {
                val now = SystemClock.elapsedRealtime()

                val waitThreshold =
                    if (lastHeartbeatTimestamp <= 0L) appHangThresholdMillis
                    else calculateTimeToAppHang(now)
                heartbeatCondition.await(waitThreshold, TimeUnit.MILLISECONDS)

                val timeSinceLastHeartbeat = now - lastHeartbeatTimestamp

                if (timeSinceLastHeartbeat >= appHangThresholdMillis) {
                    reportAppHang(timeSinceLastHeartbeat)
                }
            } catch (_: InterruptedException) {
                // Woken early by heartbeat - just continue loop
            } finally {
                heartbeatLock.unlock()
            }
        }
    }

    private inner class Heartbeat : Runnable {
        override fun run() {
            lastHeartbeatTimestamp = SystemClock.elapsedRealtime()
            // mark the hang as "recovered" and start the detection again
            isAppHangDetected = false
            resetHeartbeatTimer()

            // only post the Heartbeat messages if the monitor is still running
            if (isRunning.get()) {
                handler.post(this)
            }
        }

        override fun toString(): String {
            return "Bugsnag AppHang Heartbeat"
        }
    }
}
