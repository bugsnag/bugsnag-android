package com.bugsnag.android

import com.bugsnag.android.internal.InternalMetrics
import com.bugsnag.android.internal.InternalMetricsNoop
import java.util.concurrent.TimeUnit
import java.util.concurrent.CopyOnWriteArrayList

private const val ON_BREADCRUMB_NAME = "onBreadcrumb"
private const val ON_ERROR_NAME = "onError"
private const val ON_SEND_NAME = "onSendError"
private const val ON_SESSION_NAME = "onSession"

internal data class CallbackState(
    val onErrorTasks: MutableCollection<OnErrorCallback> = CopyOnWriteArrayList(),
    val onBreadcrumbTasks: MutableCollection<OnBreadcrumbCallback> = CopyOnWriteArrayList(),
    val onSessionTasks: MutableCollection<OnSessionCallback> = CopyOnWriteArrayList(),
    val onSendTasks: MutableList<OnSendCallback> = CopyOnWriteArrayList()
) : CallbackAware {

    internal companion object {
        private const val BREADCRUMB_CALLBACK_WARNING_THRESHOLD_MS = 1000L

        internal var nanoTimeProvider: () -> Long = System::nanoTime
    }

    private var internalMetrics: InternalMetrics = InternalMetricsNoop()

    fun setInternalMetrics(metrics: InternalMetrics) {
        internalMetrics = metrics
        internalMetrics.setCallbackCounts(getCallbackCounts())
    }

    override fun addOnError(onError: OnErrorCallback) {
        if (onErrorTasks.add(onError)) {
            internalMetrics.notifyAddCallback(ON_ERROR_NAME)
        }
    }

    override fun removeOnError(onError: OnErrorCallback) {
        if (onErrorTasks.remove(onError)) {
            internalMetrics.notifyRemoveCallback(ON_ERROR_NAME)
        }
    }

    override fun addOnBreadcrumb(onBreadcrumb: OnBreadcrumbCallback) {
        if (onBreadcrumbTasks.add(onBreadcrumb)) {
            internalMetrics.notifyAddCallback(ON_BREADCRUMB_NAME)
        }
    }

    override fun removeOnBreadcrumb(onBreadcrumb: OnBreadcrumbCallback) {
        if (onBreadcrumbTasks.remove(onBreadcrumb)) {
            internalMetrics.notifyRemoveCallback(ON_BREADCRUMB_NAME)
        }
    }

    override fun addOnSession(onSession: OnSessionCallback) {
        if (onSessionTasks.add(onSession)) {
            internalMetrics.notifyAddCallback(ON_SESSION_NAME)
        }
    }

    override fun removeOnSession(onSession: OnSessionCallback) {
        if (onSessionTasks.remove(onSession)) {
            internalMetrics.notifyRemoveCallback(ON_SESSION_NAME)
        }
    }

    fun addOnSend(onSend: OnSendCallback) {
        if (onSendTasks.add(onSend)) {
            internalMetrics.notifyAddCallback(ON_SEND_NAME)
        }
    }

    fun addPreOnSend(onSend: OnSendCallback) {
        onSendTasks.add(0, onSend)
        internalMetrics.notifyAddCallback(ON_SEND_NAME)
    }

    fun removeOnSend(onSend: OnSendCallback) {
        if (onSendTasks.remove(onSend)) {
            internalMetrics.notifyRemoveCallback(ON_SEND_NAME)
        }
    }

    fun runOnErrorTasks(event: Event, logger: Logger): Boolean {
        // optimization to avoid construction of iterator when no callbacks set
        if (onErrorTasks.isEmpty()) {
            return true
        }
        onErrorTasks.forEach {
            try {
                if (!it.onError(event)) {
                    return false
                }
            } catch (ex: Throwable) {
                logger.w("OnBreadcrumbCallback threw an Exception", ex)
            }
        }
        return true
    }

    fun runOnBreadcrumbTasks(breadcrumb: Breadcrumb, logger: Logger): Boolean {
        // optimization to avoid construction of iterator when no callbacks set
        if (onBreadcrumbTasks.isEmpty()) {
            return true
        }

        val startedAt = nanoTimeProvider()
        var result = true

        for (task in onBreadcrumbTasks) {
            try {
                if (!task.onBreadcrumb(breadcrumb)) {
                    result = false
                    break
                }
            } catch (ex: Throwable) {
                logger.w("OnBreadcrumbCallback threw an Exception", ex)
            }
        }

        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(nanoTimeProvider() - startedAt)
        if (elapsedMs >= BREADCRUMB_CALLBACK_WARNING_THRESHOLD_MS) {
            logger.w(
                "OnBreadcrumbCallback chain took ${elapsedMs}ms for ${onBreadcrumbTasks.size} callback(s)"
            )
        }

        return result
    }

    fun runOnSessionTasks(session: Session, logger: Logger): Boolean {
        // optimization to avoid construction of iterator when no callbacks set
        if (onSessionTasks.isEmpty()) {
            return true
        }
        onSessionTasks.forEach {
            try {
                if (!it.onSession(session)) {
                    return false
                }
            } catch (ex: Throwable) {
                logger.w("OnSessionCallback threw an Exception", ex)
            }
        }
        return true
    }

    fun runOnSendTasks(event: Event, logger: Logger): Boolean {
        onSendTasks.forEach {
            try {
                if (!it.onSend(event)) {
                    return false
                }
            } catch (ex: Throwable) {
                logger.w("OnSendCallback threw an Exception", ex)
            }
        }
        return true
    }

    fun runOnSendTasks(eventSource: () -> Event, logger: Logger): Boolean {
        if (onSendTasks.isEmpty()) {
            // avoid constructing event from eventSource if not needed
            return true
        }

        return this.runOnSendTasks(eventSource(), logger)
    }

    fun copy() = this.copy(
        onErrorTasks = onErrorTasks,
        onBreadcrumbTasks = onBreadcrumbTasks,
        onSessionTasks = onSessionTasks,
        onSendTasks = onSendTasks
    )

    private fun getCallbackCounts(): Map<String, Int> {
        return hashMapOf<String, Int>().also { map ->
            if (onBreadcrumbTasks.count() > 0) map[ON_BREADCRUMB_NAME] = onBreadcrumbTasks.count()
            if (onErrorTasks.count() > 0) map[ON_ERROR_NAME] = onErrorTasks.count()
            if (onSendTasks.count() > 0) map[ON_SEND_NAME] = onSendTasks.count()
            if (onSessionTasks.count() > 0) map[ON_SESSION_NAME] = onSessionTasks.count()
        }
    }
}
