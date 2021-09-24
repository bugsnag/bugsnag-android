package com.bugsnag.android

/**
 * Tracks the current notifier information and allows observers to be notified whenever it changes.
 */
internal class NotifierState : BaseObservable() {

    var notifier: Notifier = Notifier()
        set(value) {
            field = value
            updateState { StateEvent.UpdateNotifierInfo(value) }
        }

    override fun emitObservableEvent() {
        updateState {
            StateEvent.UpdateNotifierInfo(notifier)
        }
    }
}
