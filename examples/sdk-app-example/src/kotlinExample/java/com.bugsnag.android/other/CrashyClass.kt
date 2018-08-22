package com.bugsnag.android.other

object CrashyClass {

    fun crash(msg: String): RuntimeException {
        return sendMessage(msg)
    }

    private fun sendMessage(msg: String): RuntimeException {
        return RuntimeException(msg)
    }
}
