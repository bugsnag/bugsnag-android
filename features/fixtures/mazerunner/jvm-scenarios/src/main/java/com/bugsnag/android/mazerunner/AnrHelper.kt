package com.bugsnag.android.mazerunner

import android.os.Handler
import android.os.Looper
import com.bugsnag.android.OnErrorCallback

val mutex = Any()

fun createDeadlock() {
    java.lang.Thread(
        object : java.lang.Runnable {
            override fun run() {
                synchronized(mutex) {
                    while (true) {
                        try {
                            java.lang.Thread.sleep(60000)
                        } catch (e: java.lang.InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    ).start()

    Handler(Looper.getMainLooper()).postDelayed(
        object : java.lang.Runnable {
            override fun run() {
                synchronized(mutex) { throw java.lang.IllegalStateException() }
            }
        },
        1000
    )
}

// Do not allow system generated ANRs to be sent to Maze Runner
val filterSystemAnrs = OnErrorCallback { event ->
    val error = event.errors.first()
    val method1 = "android.os.BinderProxy.transact"
    val method2 = "android.app.IActivityManager\$Stub\$Proxy.handleApplicationCrash"
    if (error.errorClass.equals("ANR") &&
        error.stacktrace.any { frame -> frame.method.equals(method1) } &&
        error.stacktrace.any { frame -> frame.method.equals(method2) }
    ) {
        CiLog.info("Filtering system generated ANR")
        false
    } else {
        true
    }
}
