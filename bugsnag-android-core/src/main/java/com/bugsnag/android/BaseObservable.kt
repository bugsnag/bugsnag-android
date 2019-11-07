package com.bugsnag.android

import java.util.Observable

internal open class BaseObservable: Observable() {
    fun notifyObservers(type: NativeInterface.MessageType, arg: Any?) {
        setChanged()
        notifyObservers(NativeInterface.Message(type, arg))
    }
}
