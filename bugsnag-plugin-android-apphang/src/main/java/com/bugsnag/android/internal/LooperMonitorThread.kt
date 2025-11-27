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
    private val recoveryTimeMillis: Long,
    private val onAppHangDetected: (timeSinceLastHeartbeat: Long) -> Unit
) : Thread("Bugsnag AppHang Monitor: ${watchedLooper.thread.name}") {
    private val handler: Handler = Handler(watchedLooper)

    @Volatile
    private var lastHeartbeatTimestamp = 0L
    private var lastAppHangTimestamp = -1L

    private val isRunning = AtomicBoolean(false)

    @Volatile
    private var isCurrentlyAppHang = false

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
        if (isCurrentlyAppHang) {
            // avoid reporting duplicate AppHangs
            return
        }

        if (lastAppHangTimestamp > 0L) {
            val recoveryTimestamp = lastAppHangTimestamp + recoveryTimeMillis
            if (SystemClock.elapsedRealtime() < recoveryTimestamp) {
                // the app is not yet considered "recovered" so we mark this hang and skip it
                markAppHangDetected()
                return
            }
        }

        markAppHangDetected()
        onAppHangDetected(timeSinceLastHeartbeat)
    }

    private fun markAppHangDetected() {
        isCurrentlyAppHang = true
        lastAppHangTimestamp = SystemClock.elapsedRealtime()
    }

    override fun run() {
        handler.post(heartbeat)

        while (isRunning.get()) {
            heartbeatLock.lock()
            try {
                val waitThreshold =
                    if (lastHeartbeatTimestamp <= 0L) appHangThresholdMillis
                    else calculateTimeToAppHang(SystemClock.elapsedRealtime())

                if (waitThreshold > 0) {
                    heartbeatCondition.await(waitThreshold, TimeUnit.MILLISECONDS)
                }

                // no heartbeat received yet, so ignore this and continue the loop
                if (lastHeartbeatTimestamp <= 0) {
                    continue
                }

                val timeSinceLastHeartbeat = SystemClock.elapsedRealtime() - lastHeartbeatTimestamp

                if (timeSinceLastHeartbeat >= appHangThresholdMillis) {
                    reportAppHang(timeSinceLastHeartbeat)
                }
            } catch (_: InterruptedException) {
                // continue loop and check isRunning
            } finally {
                heartbeatLock.unlock()
            }
        }
    }

    private inner class Heartbeat : Runnable {
        override fun run() {
            lastHeartbeatTimestamp = SystemClock.elapsedRealtime()
            // mark the hang as "recovered" and start the detection again
            isCurrentlyAppHang = false

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
