package com.bugsnag.android

import android.app.ActivityManager
import android.app.ActivityManager.ProcessErrorStateInfo
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import androidx.annotation.VisibleForTesting
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal class AnrDetailsCollector {

    companion object {
        private const val INFO_POLL_THRESHOLD_MS: Long = 100
        private const val MAX_ATTEMPTS: Int = 100
    }

    private val handlerThread = HandlerThread("bugsnag-anr-collector")

    init {
        handlerThread.start()
    }

    internal fun collectAnrDetails(ctx: Context): ProcessErrorStateInfo? {
        val am = runCatching { ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager }
            .getOrNull()
        return captureProcessErrorState(am, Process.myPid())
    }

    /**
     * Collects information about an ANR, by querying an activity manager for information about
     * any proceses which are currently in an error condition.
     *
     * See https://developer.android.com/reference/android/app/ActivityManager.html#getProcessesInErrorState()
     */
    @VisibleForTesting
    internal fun captureProcessErrorState(am: ActivityManager?, pid: Int): ProcessErrorStateInfo? {
        return try {
            val processes = am?.processesInErrorState ?: emptyList()
            processes.firstOrNull { it.pid == pid }
        } catch (exc: RuntimeException) {
            null
        }
    }

    internal fun addErrorStateInfo(event: Event, anrState: ProcessErrorStateInfo) {
        val msg = anrState.shortMsg

        if (event.errors.isNotEmpty()) {
            event.errors[0].errorMessage = when {
                msg.startsWith("ANR") -> msg.replaceFirst("ANR", "")
                else -> msg
            }
        }
    }

    internal fun collectAnrErrorDetails(client: Client, event: Event) {
        val handler = Handler(handlerThread.looper)
        val attempts = AtomicInteger()
        val isMainThreadResponsive = AtomicBoolean(false)
        val mainHandler = Handler(Looper.getMainLooper())

        // 1. Post sentinel to probe Main (UI) thread responsiveness
        val sentinelRunnable = Runnable {
            isMainThreadResponsive.set(true)
        }
        mainHandler.post(sentinelRunnable)

        handler.post(
            object : Runnable {
                override fun run() {
                    // If the UI thread has processed our sentinel, it is NOT in an ANR state.
                    // Discard benign SIGQUIT false positive immediately.
                    if (isMainThreadResponsive.get()) {
                        return
                    }

                    val anrDetails = collectAnrDetails(client.appContext)

                    if (anrDetails != null) {
                        // Confirmed by OS -> Deliver immediately
                        mainHandler.removeCallbacks(sentinelRunnable)
                        addErrorStateInfo(event, anrDetails)
                        client.populateAndNotifyAndroidEvent(event, null)
                        return
                    }

                    if (attempts.getAndIncrement() < MAX_ATTEMPTS) {
                        handler.postDelayed(this, INFO_POLL_THRESHOLD_MS)
                    } else {
                        // Main thread remained un-responsive for the full polling window,
                        // confirming the hang even if ActivityManager returned null (OEM bug).
                        mainHandler.removeCallbacks(sentinelRunnable)
                        client.populateAndNotifyAndroidEvent(event, null)
                    }
                }
            }
        )
    }
}
