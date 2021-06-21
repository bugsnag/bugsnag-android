package com.bugsnag.android.anrapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AnrBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val delayTime = intent.getLongExtra("delayTime", 0L)
        withLogMessage("BroadcastReceiver delay of $delayTime seconds") {
            Thread.sleep(delayTime * 1000L)
        }
    }
}