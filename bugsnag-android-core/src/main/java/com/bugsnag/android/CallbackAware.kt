package com.bugsnag.android

internal interface CallbackAware {
    fun addOnError(onError: OnErrorCallback)
    fun removeOnError(onError: OnErrorCallback)
    fun addOnBreadcrumb(onBreadcrumb: OnBreadcrumbCallback)
    fun removeOnBreadcrumb(onBreadcrumb: OnBreadcrumbCallback)
    fun addOnSession(onSession: OnSession)
    fun removeOnSession(onSession: OnSession)
}
