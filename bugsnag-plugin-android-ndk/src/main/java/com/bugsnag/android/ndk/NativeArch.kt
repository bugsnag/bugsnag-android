package com.bugsnag.android.ndk

import android.os.Build
import androidx.annotation.VisibleForTesting

internal object NativeArch {
    @JvmField
    @VisibleForTesting
    @Suppress("ObjectPropertyNaming")
    var _is32Bit: Boolean? = null

    @JvmField
    @VisibleForTesting
    @Suppress("DEPRECATION")
    val supportedAbis: Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> Build.SUPPORTED_ABIS
        else -> arrayOf(Build.CPU_ABI, Build.CPU_ABI2)
    }

    val is32bit: Boolean
        get() {
            var result = _is32Bit
            if (result == null) {
                result = !supportedAbis.any { it.contains("64") }
                _is32Bit = result
            }

            return result
        }
}
