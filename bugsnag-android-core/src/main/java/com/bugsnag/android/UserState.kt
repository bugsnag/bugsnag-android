package com.bugsnag.android

import java.util.Observable

internal class UserState(private val userRepository: UserRepository) : Observable() {

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
        setChanged()
        notifyObservers(
            NativeInterface.Message(
                NativeInterface.MessageType.UPDATE_USER_ID, id
            )
        )
    }

    fun setUserEmail(email: String?) {
        user = user.copy(id = user.id, email = email, name = user.name)
        userRepository.save(user)
        setChanged()
        notifyObservers(
            NativeInterface.Message(
                NativeInterface.MessageType.UPDATE_USER_EMAIL, email
            )
        )
    }

    fun setUserName(name: String?) {
        user = user.copy(id = user.id, email = user.email, name = name)
        userRepository.save(user)
        setChanged()
        notifyObservers(
            NativeInterface.Message(
                NativeInterface.MessageType.UPDATE_USER_NAME, name
            )
        )
    }

}
