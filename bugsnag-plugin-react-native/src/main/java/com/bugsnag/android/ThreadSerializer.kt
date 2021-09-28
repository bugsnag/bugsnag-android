package com.bugsnag.android

internal class ThreadSerializer : MapSerializer<Thread> {
    override fun serialize(map: MutableMap<String, Any?>, thread: Thread) {
        map.putAll(thread.impl.toJournalSection())
    }
}
