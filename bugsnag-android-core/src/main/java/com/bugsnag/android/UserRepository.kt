package com.bugsnag.android

import android.content.SharedPreferences
import java.util.UUID

internal class UserRepository(private val prefs: SharedPreferences, private val persist: Boolean) {

    companion object {
        private const val INSTALL_ID_KEY = "install.iud"
        private const val USER_ID_KEY = "user.id"
        private const val USER_NAME_KEY = "user.name"
        private const val USER_EMAIL_KEY = "user.email"
    }

    fun load(): User {
        val user = User()
        var installId = prefs.getString(INSTALL_ID_KEY, null)

        if (installId == null) {
            installId = UUID.randomUUID().toString()
            prefs.edit().putString(INSTALL_ID_KEY, installId).apply()
        }
        user.installId = installId

        if (persist) {
            // Check to see if a user was stored in the SharedPreferences
            user.id = prefs.getString(USER_ID_KEY, installId)
            user.name = prefs.getString(USER_NAME_KEY, null)
            user.email = prefs.getString(USER_EMAIL_KEY, null)
        } else {
            user.id = installId
        }
        return user
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
