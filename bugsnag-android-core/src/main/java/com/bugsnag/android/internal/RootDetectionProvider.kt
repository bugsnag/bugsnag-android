package com.bugsnag.android.internal

import com.bugsnag.android.ClientObservable
import com.bugsnag.android.DeviceBuildInfo
import com.bugsnag.android.Logger
import com.bugsnag.android.RootDetector
import com.bugsnag.android.internal.dag.RunnableProvider

internal class RootDetectionProvider(
    private val deviceBuildInfo: DeviceBuildInfo,
    private val clientObservable: ClientObservable,
    private val logger: Logger,
) : RunnableProvider<Boolean>() {
    var isRooted: Boolean = false
        private set

    fun start() {
        // root detection can take 100+ms so we always have a dedicated background thread for it
        // we fire an event once we're finished to let any downstream notifiers know the result
        val worker = Thread(this, "Bugsnag Worker")
        worker.priority = Thread.MIN_PRIORITY
        worker.isDaemon = true
        worker.start()
    }

    override fun invoke(): Boolean {
        val rootDetector = RootDetector(logger = logger, deviceBuildInfo = deviceBuildInfo)
        isRooted = rootDetector.isRooted()
        clientObservable.postSynchronizeState()
        return isRooted
    }
}
