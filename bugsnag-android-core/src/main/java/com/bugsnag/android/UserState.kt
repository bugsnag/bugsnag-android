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
        user = user.copy(id = id, email = user.email, name = user.name)
        userRepository.save(user)
        notifyObservers(StateEvent.UpdateUserId(id))
    }

    fun setUserEmail(email: String?) {
        user = user.copy(id = user.id, email = email, name = user.name)
        userRepository.save(user)
        notifyObservers(StateEvent.UpdateUserEmail(email))
    }

    fun setUserName(name: String?) {
        user = user.copy(id = user.id, email = user.email, name = name)
        userRepository.save(user)
        notifyObservers(StateEvent.UpdateUserName(name))
    }

}
