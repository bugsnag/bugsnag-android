package com.bugsnag.android

internal interface CallbackAware {
    fun addOnError(onError: OnError)
    fun removeOnError(onError: OnError)
    fun addOnBreadcrumb(onBreadcrumb: OnBreadcrumbCallback)
    fun removeOnBreadcrumb(onBreadcrumb: OnBreadcrumbCallback)
    fun addOnSession(onSession: OnSession)
    fun removeOnSession(onSession: OnSession)
}
