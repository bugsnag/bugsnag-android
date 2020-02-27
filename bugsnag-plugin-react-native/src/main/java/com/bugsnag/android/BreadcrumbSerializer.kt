package com.bugsnag.android

import java.util.Locale

class BreadcrumbSerializer : WritableMapSerializer<Breadcrumb> {
    override fun serialize(map: MutableMap<String, Any?>, crumb: Breadcrumb) {
        map["timestamp"] = DateUtils.toIso8601(crumb.timestamp)
        map["message"] = crumb.message
        map["type"] = crumb.type.toString().toLowerCase(Locale.US)
        map["metadata"] = crumb.metadata
    }
}
