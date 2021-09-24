package com.bugsnag.android

internal class UserState(user: User) : BaseObservable() {
    var user = user
        set(value) {
            field = value
            emitObservableEvent()
        }

    override fun emitObservableEvent() = updateState { StateEvent.UpdateUser(user) }
}
