package com.bugsnag.android

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import java.util.concurrent.Callable

/**
 * Reads legacy information left in SharedPreferences and migrates it to the new location.
 */
internal class SharedPrefMigrator(private val context: Context) :
    DeviceIdPersistence,
    Callable<SharedPrefMigrator> {

    private var installId: String? = null
    private var userId: String? = null
    private var userEmail: String? = null
    private var userName: String? = null

    override fun call(): SharedPrefMigrator {
        try {
            val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
                ?: return this

            installId = prefs.getString(INSTALL_ID_KEY, null)
            userId = prefs.getString(USER_ID_KEY, null)
            userEmail = prefs.getString(USER_EMAIL_KEY, null)
            userName = prefs.getString(USER_NAME_KEY, null)

            @SuppressLint("ApplySharedPref")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(SHARED_PREFS_NAME)
            } else {
                prefs.edit().clear().commit()
            }
        } catch (_: RuntimeException) {
        }

        return this
    }

    /**
     * This implementation will never create an ID; it will only fetch one if present.
     */
    override fun loadDeviceId(requestCreateIfDoesNotExist: Boolean) = installId

    fun loadUser(deviceId: String?) = User(
        userId ?: deviceId,
        userEmail,
        userName
    )

    fun hasPrefs() = installId != null

    companion object {
        private const val INSTALL_ID_KEY = "install.iud"
        private const val USER_ID_KEY = "user.id"
        private const val USER_NAME_KEY = "user.name"
        private const val USER_EMAIL_KEY = "user.email"
        private const val SHARED_PREFS_NAME = "com.bugsnag.android"
    }
}
