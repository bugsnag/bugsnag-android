package com.bugsnag.android.internal

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import java.lang.ref.WeakReference
import kotlin.math.max

internal object ForegroundDetector : ActivityLifecycleCallbacks {

    /**
     * Same as `androidx.lifecycle.ProcessLifecycleOwner` and is used to avoid reporting
     * background / foreground changes when there is only 1 Activity being restarted for configuration
     * changes.
     */
    @VisibleForTesting
    internal const val BACKGROUND_TIMEOUT_MS = 700L

    /**
     * We weak-ref all of the listeners to avoid keeping Client instances around forever. The
     * references are cleaned up each time we iterate over the list to notify the listeners.
     */
    private val listeners = ArrayList<WeakReference<OnActivityCallback>>()

    private val mainThreadHandler = Handler(Looper.getMainLooper())

    private var observedApplication: Application? = null

    /**
     * The number of Activity instances: `onActivityCreated` - `onActivityDestroyed`
     */
    private var activityInstanceCount: Int = 0

    /**
     * The number of started Activity instances: `onActivityStarted` - `onActivityStopped`
     */
    private var startedActivityCount: Int = 0

    private var backgroundSent = true

    var isInForeground: Boolean = false
        private set

    private val sendInBackground: Runnable = Runnable {
        if (!backgroundSent) {
            isInForeground = false
            backgroundSent = true

            notifyListeners { it.onForegroundStatus(false) }
        }
    }

    @JvmStatic
    fun registerOn(application: Application) {
        if (application === observedApplication) {
            return
        }

        observedApplication?.unregisterActivityLifecycleCallbacks(this)
        observedApplication = application
        application.registerActivityLifecycleCallbacks(this)
    }

    @JvmStatic
    fun registerActivityCallbacks(callbacks: OnActivityCallback) {
        synchronized(listeners) {
            listeners.add(WeakReference(callbacks))
        }
    }

    private inline fun notifyListeners(sendCallback: (OnActivityCallback) -> Unit) {
        synchronized(listeners) {
            if (listeners.isEmpty()) {
                return
            }

            try {
                val iterator = listeners.iterator()
                while (iterator.hasNext()) {
                    val ref = iterator.next()
                    val listener = ref.get()
                    if (listener == null) {
                        iterator.remove()
                    } else {
                        sendCallback(listener)
                    }
                }
            } catch (e: Exception) {
                // ignore callback errors
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activityInstanceCount++
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivityCount++
        mainThreadHandler.removeCallbacks(sendInBackground)
        isInForeground = true

        notifyListeners { it.onForegroundStatus(true) }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            notifyListeners { it.onActivityStarted(activity) }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount = max(0, startedActivityCount - 1)

        if (startedActivityCount == 0) {
            backgroundSent = false
            mainThreadHandler.postDelayed(sendInBackground, BACKGROUND_TIMEOUT_MS)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            notifyListeners { it.onActivityStopped(activity) }
        }
    }

    override fun onActivityPostStarted(activity: Activity) {
        notifyListeners { it.onActivityStarted(activity) }
    }

    override fun onActivityPostStopped(activity: Activity) {
        notifyListeners { it.onActivityStopped(activity) }
    }

    override fun onActivityDestroyed(activity: Activity) {
        activityInstanceCount = max(0, activityInstanceCount - 1)
    }

    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    interface OnActivityCallback {
        fun onForegroundStatus(foreground: Boolean)

        fun onActivityStarted(activity: Activity)

        fun onActivityStopped(activity: Activity)
    }
}
