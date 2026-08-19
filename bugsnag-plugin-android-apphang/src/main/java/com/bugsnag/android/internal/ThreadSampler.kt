package com.bugsnag.android.internal

import com.bugsnag.android.Event

internal class ThreadSampler(
    val thread: Thread,
) {
    private val sampleRoot: StackTreeNode = StackTreeNode()

    val totalSamplesTaken: Int
        get() = sampleRoot.sampleCount

    fun createError(event: Event) {
        val error = event.addError(
            "AppHang",
            "Most frequent stack path ($totalSamplesTaken samples taken)"
        )

        var node = sampleRoot.mostSampledChildNode()
        while (node != null) {
            val className = node.className
            val methodName = when {
                className.isNotEmpty() -> className + "." + node.methodName
                else -> node.methodName
            }

            error.addStackframe(
                methodName,
                node.fileName,
                node.lineNumber.toLong()
            )

            node = node.mostSampledChildNode()
        }

        // reverse the stacktrace so it's in the correct order
        error.stacktrace.reverse()
    }

    fun resetSampling() {
        sampleRoot.clear()
    }

    fun captureSample() {
        val stackTrace = thread.stackTrace

        if (stackTrace.isEmpty()) {
            return
        }

        var current: StackTreeNode = sampleRoot
        current.sampleCount++
        // we walk the stacktrace in reverse
        for (i in stackTrace.lastIndex downTo 0) {
            val next = current.childNodeFor(stackTrace[i])
            next.sampleCount++
            current = next
        }
    }
}
