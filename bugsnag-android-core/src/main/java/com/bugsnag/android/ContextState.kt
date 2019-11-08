package com.bugsnag.android

internal class ContextState(context: String? = null) : BaseObservable() {
    var context = context
        set(value) {
            field = value
            notifyObservers(StateEvent.UpdateContext(context))
        }

    fun copy() = ContextState(context)
}
