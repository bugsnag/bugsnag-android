package com.bugsnag.android

import java.util.concurrent.ConcurrentLinkedQueue

internal data class ClientState(
    val metadata: MetaData = MetaData(),
    var context: String? = null,
    val onErrorTasks: MutableCollection<OnError> = ConcurrentLinkedQueue<OnError>(),
    val onBreadcrumbTasks: MutableCollection<OnBreadcrumb> = ConcurrentLinkedQueue<OnBreadcrumb>(),
    val onSessionTasks: MutableCollection<OnSession> = ConcurrentLinkedQueue()
) {

    fun addOnError(onError: OnError) {
        if (!onErrorTasks.contains(onError)) {
            onErrorTasks.add(onError)
        }
    }

    fun removeOnError(onError: OnError) {
        onErrorTasks.remove(onError)
    }

    fun addOnBreadcrumb(onBreadcrumb: OnBreadcrumb) {
        if (!onBreadcrumbTasks.contains(onBreadcrumb)) {
            onBreadcrumbTasks.add(onBreadcrumb)
        }
    }

    fun removeOnBreadcrumb(onBreadcrumb: OnBreadcrumb) {
        onBreadcrumbTasks.remove(onBreadcrumb)
    }

    fun addOnSession(onSession: OnSession) {
        if (!onSessionTasks.contains(onSession)) {
            onSessionTasks.add(onSession)
        }
    }

    fun removeOnSession(onSession: OnSession) {
        onSessionTasks.remove(onSession)
    }

    fun copy() = this.copy(
        metadata = metadata,
        context = context,
        onErrorTasks = onErrorTasks,
        onBreadcrumbTasks = onBreadcrumbTasks,
        onSessionTasks = onSessionTasks
    )
}
