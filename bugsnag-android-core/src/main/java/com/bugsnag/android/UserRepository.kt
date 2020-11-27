package com.bugsnag.android

import android.content.SharedPreferences
import java.util.UUID

internal class UserRepository(
    private val prefs: SharedPreferences,
    private val persist: Boolean,
    private val deviceId: String?
) {

    companion object {
        private const val USER_ID_KEY = "user.id"
        private const val USER_NAME_KEY = "user.name"
        private const val USER_EMAIL_KEY = "user.email"
    }

    fun load(): User {
        return when {
            persist -> // Check to see if a user was stored in the SharedPreferences
                User(
                    prefs.getString(USER_ID_KEY, deviceId),
                    prefs.getString(USER_EMAIL_KEY, null),
                    prefs.getString(USER_NAME_KEY, null)
                )
            else -> User(deviceId, null, null)
        }
    }

    fun save(user: User) {
        val editor = prefs.edit()
        if (persist) {
            editor
                .putString(USER_ID_KEY, user.id)
                .putString(USER_NAME_KEY, user.name)
                .putString(USER_EMAIL_KEY, user.email)
        } else {
            editor
                .remove(USER_ID_KEY)
                .remove(USER_NAME_KEY)
                .remove(USER_EMAIL_KEY)
        }
        editor.apply()
    }
}
