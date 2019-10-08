package com.bugsnag.android.ndk

import java.lang.IllegalStateException

internal fun verifyNativeRun(code: Int): Boolean {
    if (code != 0) {
        throw IllegalStateException("Native tests failed. Check device logs for results.")
    } else {
        return true
    }
}