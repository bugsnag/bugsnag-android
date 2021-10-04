package com.bugsnag.android

internal class AppSerializer : MapSerializer<AppWithState> {
    override fun serialize(map: MutableMap<String, Any?>, app: AppWithState) {
        map.putAll(app.toJournalSection())
        map["buildUuid"] = map["buildUUID"]
        map.remove("buildUUID")
    }
}
