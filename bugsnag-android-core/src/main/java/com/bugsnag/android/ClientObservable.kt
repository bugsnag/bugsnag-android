package com.bugsnag.android

import java.util.ArrayList
import java.util.Observable

internal class ClientObservable : Observable() {

    fun postOrientationChange(orientation: Int) {
        setChanged()
        notifyObservers(
            NativeInterface.Message(
                NativeInterface.MessageType.UPDATE_ORIENTATION, orientation
            )
        )
    }

    fun postNdkInstall(immutableConfig: ImmutableConfig) {
        setChanged()
        val messageArgs = ArrayList<Any>()
        messageArgs.add(immutableConfig.autoDetectNdkCrashes)

        super.notifyObservers(
            NativeInterface.Message(NativeInterface.MessageType.INSTALL, messageArgs)
        )
    }

    fun postNdkDeliverPending() {
        setChanged()
        notifyObservers(
            NativeInterface.Message(NativeInterface.MessageType.DELIVER_PENDING, null)
        )
    }
}
