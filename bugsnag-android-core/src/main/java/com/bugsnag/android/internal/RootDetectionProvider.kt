package com.bugsnag.android.internal

import com.bugsnag.android.ClientObservable
import com.bugsnag.android.DeviceBuildInfo
import com.bugsnag.android.Logger
import com.bugsnag.android.RootDetector
import com.bugsnag.android.internal.dag.RunnableProvider
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class RootDetectionProvider(
    private val deviceBuildInfo: DeviceBuildInfo,
    private val clientObservable: ClientObservable,
    private val logger: Logger,
) : RunnableProvider<Boolean>() {
    private val rootDetectionResult = AtomicReference<Boolean?>(null)
    private val unknownResultReturned = AtomicBoolean(false)

    /**
     * Returns the cached root-detection result, calculating it on the calling thread when no
     * background calculation has started. Only the first lookup while the background calculation
     * is running returns null; later lookups wait for the cached result.
     */
    fun getRootDetectionResult(): Boolean? {
        if (rootDetectionResult.get() == null) {
            run()
        }
        return rootDetectionResult.get()
            ?: if (unknownResultReturned.compareAndSet(false, true)) null else getOrNull()
    }

    /**
     * Starts a best-effort calculation after startup. This must not be invoked on the startup
     * critical path; error capture can calculate the result synchronously when needed.
     */
    fun startInBackground() {
        val worker = Thread(this, "Bugsnag Worker")
        worker.priority = Thread.MIN_PRIORITY
        worker.isDaemon = true
        worker.start()
    }

    override fun invoke(): Boolean {
        val rootDetector = RootDetector(logger = logger, deviceBuildInfo = deviceBuildInfo)
        val isRooted = rootDetector.isRooted()
        rootDetectionResult.set(isRooted)
        clientObservable.postSynchronizeState()
        return isRooted
    }
}
