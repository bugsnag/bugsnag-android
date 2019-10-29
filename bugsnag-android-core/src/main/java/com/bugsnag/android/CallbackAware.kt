package com.bugsnag.android

internal interface CallbackAware {
    fun addOnError(onError: OnError)
    fun removeOnError(onError: OnError)
    fun addOnBreadcrumb(onBreadcrumb: OnBreadcrumb)
    fun removeOnBreadcrumb(onBreadcrumb: OnBreadcrumb)
    fun addOnSession(onSession: OnSession)
    fun removeOnSession(onSession: OnSession)
}
