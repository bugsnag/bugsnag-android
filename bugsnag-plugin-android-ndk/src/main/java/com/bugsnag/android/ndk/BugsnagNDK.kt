package com.bugsnag.android.ndk

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.ndkPlugin

object BugsnagNDK {
    @JvmStatic
    fun refreshSymbolTable() {
        if (Bugsnag.isStarted()) {
            val client = Bugsnag.getClient()
            client.ndkPlugin?.nativeBridge?.refreshSymbolTable()
        }
    }
}
