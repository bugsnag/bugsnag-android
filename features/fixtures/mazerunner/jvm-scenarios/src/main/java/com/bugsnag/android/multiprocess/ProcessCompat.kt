package com.bugsnag.android.multiprocess

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import com.bugsnag.android.Configuration
import java.lang.reflect.Method

/**
 * Retrieves the name of the current process. This is used to set the persistenceDirectory on the
 * [Configuration] class to a unique value for each process.
 */
fun findCurrentProcessName(): String {
    return runCatching {
        when {
            VERSION.SDK_INT >= VERSION_CODES.P -> {
                Application.getProcessName()
            }
            else -> {
                // see https://stackoverflow.com/questions/19631894
                @SuppressLint("PrivateApi")
                val clz = Class.forName("android.app.ActivityThread")
                val methodName = when {
                    VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2 -> "currentProcessName"
                    else -> "currentPackageName"
                }

                val getProcessName: Method = clz.getDeclaredMethod(methodName)
                getProcessName.invoke(null) as String
            }
        }
    }.getOrThrow()
}
