package com.bugsnag.android

internal class ContextState(context: String? = null) : BaseObservable() {
    var context = context
        set(value) {
            field = value
            emitObservableEvent()
        }

    fun emitObservableEvent() = updateState { StateEvent.UpdateContext(context) }

    fun copy() = ContextState(context)
}
