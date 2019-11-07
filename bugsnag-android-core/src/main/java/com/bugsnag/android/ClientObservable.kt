package com.bugsnag.android

import java.util.ArrayList
import java.util.Observable

internal class ClientObservable : BaseObservable() {

    fun postOrientationChange(orientation: Int) {
        notifyObservers(NativeInterface.MessageType.UPDATE_ORIENTATION, orientation)
    }

    fun postNdkInstall(immutableConfig: ImmutableConfig) {
        val messageArgs = ArrayList<Any>()
        messageArgs.add(immutableConfig.autoDetectNdkCrashes)
        messageArgs.add(immutableConfig.appVersion ?: "")
        messageArgs.add(immutableConfig.buildUuid ?: "")
        messageArgs.add(immutableConfig.releaseStage ?: "")
        notifyObservers(NativeInterface.MessageType.INSTALL, messageArgs)
    }

    fun postNdkDeliverPending() {
        notifyObservers(NativeInterface.MessageType.DELIVER_PENDING, null)
    }
}
