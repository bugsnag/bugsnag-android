package com.bugsnag.android

import android.os.Handler
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.lang.Thread as JThread

/**
 * An alternative to Application Not Responding (ANR) reporting with configurable timeouts.
 */
class BugsnagAppHangPlugin @JvmOverloads constructor(
    configuration: AppHangConfiguration = AppHangConfiguration()
) : Plugin {
    private val appHangThresholdMillis = configuration.appHangThresholdMillis
    private val watchedLooper = configuration.watchedLooper

    private lateinit var handler: Handler

    private var client: Client? = null
    private var monitorThread: JThread? = null

    private var lastHeartbeatTimestamp = 0L

    private val isRunning = AtomicBoolean(false)

    private var isAppHangDetected = false

    private val heartbeatLock = ReentrantLock(false)
    private val heartbeatCondition = heartbeatLock.newCondition()

    private val heartbeat: Runnable = Heartbeat()

    override fun load(client: Client) {
        this.client = client
        this.handler = Handler(watchedLooper)
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

    internal fun resetHeartbeatTimer() {
        heartbeatLock.lock()
        try {
            heartbeatCondition.signalAll()
        } finally {
            heartbeatLock.unlock()
        }
    }

    private fun reportAppHang(timeSinceLastHeartbeat: Long) {
        isAppHangDetected = true

        val watchedThread = watchedLooper.thread
        val stackTrace = watchedThread.stackTrace
        val threadName = watchedThread.name
        client?.notify(
            AppHangException(
                "$threadName has not responded in ${timeSinceLastHeartbeat}ms",
                stackTrace
            )
        )
    }

    @VisibleForTesting
    internal fun startMonitoring() {
        if (!isRunning.compareAndSet(false, true)) {
            return
        }

        monitorThread = JThread {
            while (isRunning.get()) {
                heartbeatLock.lock()
                try {
                    val now = SystemClock.elapsedRealtime()

                    val waitThreshold =
                        if (lastHeartbeatTimestamp <= 0L) appHangThresholdMillis
                        else calculateTimeToAppHang(now)
                    heartbeatCondition.await(waitThreshold, TimeUnit.MILLISECONDS)

                    val timeSinceLastHeartbeat = now - lastHeartbeatTimestamp

                    if (timeSinceLastHeartbeat >= appHangThresholdMillis &&
                        // we always wait for until the nextAppHangDetectionTimestamp has passed before
                        !isAppHangDetected
                    ) {
                        reportAppHang(timeSinceLastHeartbeat)
                    }
                } catch (_: InterruptedException) {
                    // Woken early by heartbeat - just continue loop
                } finally {
                    heartbeatLock.unlock()
                }
            }
        }.apply {
            name = "Bugsnag AppHang Monitor"
            start()
        }

        handler.post(heartbeat)
    }

    @VisibleForTesting
    internal fun stopMonitoring() {
        if (isRunning.compareAndSet(true, false)) {
            monitorThread?.interrupt()
            monitorThread = null
        }
    }

    private fun calculateTimeToAppHang(now: Long): Long =
        (lastHeartbeatTimestamp + appHangThresholdMillis) - now

    private inner class Heartbeat : Runnable {
        override fun run() {
            lastHeartbeatTimestamp = SystemClock.elapsedRealtime()
            // mark the hang as "recovered" and start the detection again
            isAppHangDetected = false
            resetHeartbeatTimer()
            handler.post(this)
        }

        override fun toString(): String {
            return "Bugsnag AppHang Heartbeat"
        }
    }
}
