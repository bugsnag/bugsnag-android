package com.bugsnag.android

internal class ContextState(context: String? = null) : BaseObservable() {
    var context = context
        set(value) {
            field = value
            notifyObservers(NativeInterface.MessageType.UPDATE_CONTEXT, context)
        }

    fun copy() = ContextState(context)
}
