package com.bugsnag.android

internal fun Client.shouldDiscardNetworkBreadcrumb() =
    config.shouldDiscardBreadcrumb(BreadcrumbType.REQUEST)

internal val Client.log
    get() = this.logger
