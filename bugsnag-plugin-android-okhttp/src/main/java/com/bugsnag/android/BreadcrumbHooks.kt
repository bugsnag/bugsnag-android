package com.bugsnag.android

internal fun Client.shouldDiscardNetworkBreadcrumb() = config.shouldDiscardBreadcrumb(BreadcrumbType.REQUEST)
