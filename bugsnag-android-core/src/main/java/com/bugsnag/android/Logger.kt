package com.bugsnag.android

interface Logger {
    fun e(msg: String): Unit = Unit
    fun e(msg: String, throwable: Throwable): Unit = Unit
    fun w(msg: String): Unit = Unit
    fun w(msg: String, throwable: Throwable): Unit = Unit
    fun i(msg: String): Unit = Unit
    fun i(msg: String, throwable: Throwable): Unit = Unit
    fun d(msg: String): Unit = Unit
    fun d(msg: String, throwable: Throwable): Unit = Unit
}
