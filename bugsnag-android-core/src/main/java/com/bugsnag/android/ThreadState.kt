package com.bugsnag.android

import java.io.IOException

/**
 * Capture and serialize the state of all threads at the time of an exception.
 */
internal class ThreadState @JvmOverloads constructor
    (
    config: ImmutableConfig,
    exc: Throwable?,
    currentThread: java.lang.Thread = java.lang.Thread.currentThread(),
    stackTraces: MutableMap<java.lang.Thread, Array<StackTraceElement>> = java.lang.Thread.getAllStackTraces(),
    logger: Logger = config.logger
) : JsonStream.Streamable {

    internal val threads: MutableList<Thread>

    init {
        // API 24/25 don't record the currentThread, add it in manually
        // https://issuetracker.google.com/issues/64122757
        if (!stackTraces.containsKey(currentThread)) {
            stackTraces[currentThread] = currentThread.stackTrace
        }
        if (exc != null) { // unhandled errors use the exception trace for thread traces
            stackTraces[currentThread] = exc.stackTrace
        }

        val currentThreadId = currentThread.id
        threads = stackTraces.keys
            .sortedBy { it.id }
            .map {
                val stacktrace = Stacktrace(stackTraces[it]!!, config.projectPackages, logger)
                Thread(it.id, it.name, Thread.Type.ANDROID, it.id == currentThreadId, stacktrace)
            }.toMutableList()
    }

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginArray()
        for (thread in threads) {
            writer.value(thread)
        }
        writer.endArray()
    }
}
