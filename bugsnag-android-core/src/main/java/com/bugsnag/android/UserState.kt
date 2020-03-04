package com.bugsnag.android

internal class UserState(private val userRepository: UserRepository) : BaseObservable() {

    var user = userRepository.load()
        private set

    fun setUser(id: String?, email: String?, name: String?) {
        user = User(id, email, name)
        userRepository.save(user)
        notifyObservers(StateEvent.UpdateUser(user))
    }

}
