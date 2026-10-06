package com.bugsnag.android.mazerunner

import android.content.SharedPreferences

fun SharedPreferences.setStoredApiKey(apiKeyKey: String, apiKey: String) {
    with(edit()) {
        putString(apiKeyKey, apiKey)
        commit()
    }
}

fun SharedPreferences.clearStoredApiKey(apiKeyKey: String) {
    with(edit()) {
        remove(apiKeyKey)
        commit()
    }
}

fun SharedPreferences.getStoredApiKey(apiKeyKey: String): String? {
    return getString(apiKeyKey, "")
}
