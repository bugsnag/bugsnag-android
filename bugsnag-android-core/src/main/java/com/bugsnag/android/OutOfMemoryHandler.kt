package com.bugsnag.android

fun interface OutOfMemoryHandler {
    /**
     * Called when an `OutOfMemoryError` is reported but before it is handled by Bugsnag. This
     * can be used to either fully-process the `OutOfMemoryError` on a safe path, or can
     * attempt to free more memory to allow the `OutOfMemoryError` error to be processed and
     * reported (as a normal [Event]).
     *
     * @return true if the `OutOfMemoryError` was fully reported by this handler, `false` if normal
     *  error reporting should continue
     */
    fun onOutOfMemory(oom: OutOfMemoryError): Boolean
}
