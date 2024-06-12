package com.bugsnag.android.mazerunner

import android.os.Handler
import android.os.Looper

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
