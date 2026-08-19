package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig

internal class ClientObservable : BaseObservable() {

    fun postOrientationChange(orientation: String?) {
        updateState { StateEvent.UpdateOrientation(orientation) }
    }

    fun postNdkInstall(
        conf: ImmutableConfig,
        lastRunInfoPath: String,
        consecutiveLaunchCrashes: Int
    ) {
        updateState {
            StateEvent.Install(
                conf.apiKey,
                conf.enabledErrorTypes.ndkCrashes,
                conf.appVersion,
                if (conf.buildUuid?.isComplete == true) conf.buildUuid.getOrNull()
                else null,
                conf.releaseStage,
                lastRunInfoPath,
                consecutiveLaunchCrashes,
                conf.sendThreads,
                conf.maxBreadcrumbs
            )
        }
    }

    fun postNdkDeliverPending() {
        updateState { StateEvent.DeliverPending }
    }

    fun postSynchronizeState() {
        updateState { StateEvent.SynchronizeState }
    }
}
