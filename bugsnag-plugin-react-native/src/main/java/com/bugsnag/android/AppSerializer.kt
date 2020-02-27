package com.bugsnag.android

class AppSerializer : WritableMapSerializer<AppWithState> {
    override fun serialize(map: MutableMap<String, Any?>, app: AppWithState) {
        map["appType"] = app.type
        map["binaryArch"] = app.binaryArch
        map["buildUuid"] = app.buildUuid
        map["codeBundleId"] = app.codeBundleId
        map["duration"] = app.duration
        map["durationInForeground"] = app.durationInForeground
        map["id"] = app.id
        map["inForeground"] = app.inForeground
        map["releaseStage"] = app.releaseStage
        map["version"] = app.version
        map["versionCode"] = app.versionCode
    }
}
