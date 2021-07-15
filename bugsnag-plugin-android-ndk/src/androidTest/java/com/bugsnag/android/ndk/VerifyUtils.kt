package com.bugsnag.android.ndk

import java.lang.IllegalStateException

internal fun verifyNativeRun(code: Int): Boolean {
    if (code != 0) {
        throw IllegalStateException(
            "Native test suite failed." +
                " Filter the device logs by 'BugsnagNDKTest' to get the results."
        )
    } else {
        return true
    }
}
