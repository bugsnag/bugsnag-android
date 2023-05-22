package com.bugsnag.android

import android.os.Build

inline fun <R> withBuildSdkInt(sdk: Int, block: () -> R): R {
    val build = Build.VERSION::class.java
    val sdkInt = build.getDeclaredField("SDK_INT")
        .apply { isAccessible = true }

    val oldSdkInt = sdkInt.get(null)
    sdkInt.set(null, sdk)
    try {
        return block()
    } finally {
        sdkInt.set(null, oldSdkInt)
    }
}
