package com.bugsnag.android

import androidx.navigation.NavController

class BugsnagNavigationPlugin(
    val navc: NavController
) : Plugin {
    private val breadcrumbType: BreadcrumbType = BreadcrumbType.NAVIGATION

    override fun load(client: Client) {
        navc.addOnDestinationChangedListener { _, destination, _ ->
            val context = destination.label?.toString() ?: "Unknown"
            Bugsnag.client.sessionTracker.updateContext(context, true)
            Bugsnag.leaveBreadcrumb("Test for Navigation", mapOf("Navigation breadcrumb" to 11111111111), breadcrumbType)
        }
    }

    override fun unload() = Unit
}
