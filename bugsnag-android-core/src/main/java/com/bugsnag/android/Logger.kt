package com.bugsnag.android

interface Logger {
    fun e(msg: String) = Unit
    fun e(msg: String, throwable: Throwable) = Unit
    fun w(msg: String) = Unit
    fun w(msg: String, throwable: Throwable) = Unit
    fun i(msg: String) = Unit
    fun i(msg: String, throwable: Throwable) = Unit
    fun d(msg: String) = Unit
    fun d(msg: String, throwable: Throwable) = Unit
}
