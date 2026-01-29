@file:JvmName("ThrowableUtils")
package com.bugsnag.android

/**
 * The maximum number of causes to consider when unrolling the cause chain for a Throwable.
 */
private const val MAX_CAUSE_COUNT = 100

/**
 * Unroll the list of causes for this Throwable, handling any recursion that may appear within
 * the chain. The first element returned will be this Throwable, and the last will be the root
 * cause or last non-recursive Throwable.
 */
internal fun Throwable.safeUnrollCauses(): List<Throwable> {
    val causes = LinkedHashSet<Throwable>()
    var currentEx: Throwable? = this

    // Set.add will return false if we have already "seen" currentEx
    while (currentEx != null && causes.add(currentEx)) {
        currentEx = currentEx.cause
    }

    return causes.toList()
}

internal inline fun Throwable.anyCauseMatches(action: (Throwable) -> Boolean): Boolean {
    var current: Throwable? = this
    var slow: Throwable? = this
    var advanceSlow = false
    var depth = 0

    while (current != null && depth < MAX_CAUSE_COUNT) {
        if (action(current)) {
            return true
        }

        current = current.cause

        // Floyd's cycle detection: move slow pointer every other iteration
        if (advanceSlow) {
            slow = slow?.cause
            if (current === slow) {
                return false
            }
        }
        advanceSlow = !advanceSlow
        depth++
    }

    return false
}
