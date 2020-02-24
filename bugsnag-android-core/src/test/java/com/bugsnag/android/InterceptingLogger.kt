package com.bugsnag.android

class InterceptingLogger : Logger {

    var msg: String? = null

    override fun w(msg: String) {
        this.msg = msg
    }

    override fun w(msg: String, throwable: Throwable) {
        this.msg = msg
    }

    override fun e(msg: String) {
        this.msg = msg
    }

    override fun e(msg: String, throwable: Throwable) {
        this.msg = msg
    }
}
