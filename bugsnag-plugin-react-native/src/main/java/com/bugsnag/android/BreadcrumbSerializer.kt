package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
import java.util.Locale

internal class BreadcrumbSerializer : MapSerializer<Breadcrumb> {
    override fun serialize(map: MutableMap<String, Any?>, crumb: Breadcrumb) {
        map["timestamp"] = DateUtils.toIso8601(crumb.timestamp)
        map["message"] = crumb.message
        map["type"] = crumb.type.toString().toLowerCase(Locale.US)
        map["metadata"] = crumb.metadata
    }
}
