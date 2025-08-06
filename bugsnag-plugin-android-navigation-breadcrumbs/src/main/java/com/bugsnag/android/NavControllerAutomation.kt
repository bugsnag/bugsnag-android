package com.bugsnag.android

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.navigation.NavHost

class NavControllerAutomation : Application.ActivityLifecycleCallbacks,
    FragmentLifecycleCallbacks() {

    private val breadcrumbType: BreadcrumbType = BreadcrumbType.NAVIGATION

    override fun onActivityCreated(activity: Activity, p1: Bundle?) {
        if (activity is FragmentActivity) {
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(this, true)
        }
    }

    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
        if (f is NavHost) {
            f.navController.addOnDestinationChangedListener { _, destination, _ ->
                val context = destination.label?.toString() ?: "Unknown"
                Bugsnag.client.sessionTracker.updateContext(context, true)
                Bugsnag.leaveBreadcrumb(
                    "Test for Navigation",
                    mapOf("Navigation breadcrumb" to 11111111111),
                    breadcrumbType
                )
            }
        }
    }

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}