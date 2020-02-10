package com.bugsnag.android

internal class ClientObservable : BaseObservable() {

    fun postOrientationChange(orientation: String?) {
        notifyObservers(StateEvent.UpdateOrientation(orientation))
    }

    fun postNdkInstall(conf: ImmutableConfig) {
        notifyObservers(
            StateEvent.Install(
                conf.enabledErrorTypes.ndkCrashes, conf.appVersion,
                conf.buildUuid, conf.releaseStage
            )
        )
    }

    fun postNdkDeliverPending() {
        notifyObservers(StateEvent.DeliverPending)
    }
}
