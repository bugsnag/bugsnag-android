package com.bugsnag.android.ndk

internal fun verifyNativeRun(code: Int): Boolean {
    check(code == 0) { "Native tests failed. Check device logs for results." }
    return true
}
