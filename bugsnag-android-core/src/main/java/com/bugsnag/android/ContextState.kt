package com.bugsnag.android

import java.util.Observable

internal class ContextState(context: String? = null) : Observable() {
    var context = context
        set(value) {
            field = value
            setChanged()
            notifyObservers(
                NativeInterface.Message(
                    NativeInterface.MessageType.UPDATE_CONTEXT,
                    context
                )
            )
        }

    fun copy() = ContextState(context)
}
