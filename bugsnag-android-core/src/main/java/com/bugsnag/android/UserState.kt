package com.bugsnag.android

internal class UserState(private val userRepository: UserRepository) : BaseObservable() {

    var user = userRepository.load()
        private set

    fun setUser(id: String?, email: String?, name: String?) {
        setUserId(id)
        setUserEmail(email)
        setUserName(name)
    }

    fun setUserId(id: String?) {
        user = User(id, user.email, user.name)
        userRepository.save(user)
        notifyObservers(StateEvent.UpdateUserId(id))
    }

    fun setUserEmail(email: String?) {
        user = User(user.id, email, user.name)
        userRepository.save(user)
        notifyObservers(StateEvent.UpdateUserEmail(email))
    }

    fun setUserName(name: String?) {
        user = User(user.id, user.email, name)
        userRepository.save(user)
        notifyObservers(StateEvent.UpdateUserName(name))
    }

}
