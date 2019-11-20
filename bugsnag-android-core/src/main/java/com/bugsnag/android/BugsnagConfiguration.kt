package com.bugsnag.android

interface BugsnagConfiguration {
    fun setContext(context: String?)
    fun getContext(): String?
    fun addOnError(onError: OnError)
}
