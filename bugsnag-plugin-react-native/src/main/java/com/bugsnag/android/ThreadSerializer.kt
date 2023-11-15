package com.bugsnag.android

internal class ThreadSerializer : MapSerializer<Thread> {
    override fun serialize(map: MutableMap<String, Any?>, thread: Thread) {
        map["id"] = thread.id
        map["name"] = thread.name
        map["type"] = thread.type.toString().lowercase()
        map["errorReportingThread"] = thread.errorReportingThread
        map["state"] = thread.state.descriptor

        map["stacktrace"] = thread.stacktrace.map {
            val frame = mutableMapOf<String, Any?>()
            frame["method"] = it.method
            frame["lineNumber"] = it.lineNumber
            frame["file"] = it.file
            frame["inProject"] = it.inProject
            frame
        }
    }
}
