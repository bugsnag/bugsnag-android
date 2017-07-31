package com.bugsnag.android.other

class Other {

    fun meow() {
        mew()
    }

    private fun mew() {
        throw RuntimeException("herpaderpa")
    }
}
