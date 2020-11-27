package com.bugsnag.android

import android.content.Context

/**
 * Reads legacy information left in SharedPreferences and migrates it to the new location.
 */
internal class SharedPrefMigrator(context: Context) {

    private val prefs = context
        .getSharedPreferences("com.bugsnag.android", Context.MODE_PRIVATE)

    fun loadDeviceId() = prefs.getString(INSTALL_ID_KEY, null)

    companion object {
        private const val INSTALL_ID_KEY = "install.iud"
    }
}
