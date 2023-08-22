package com.bugsnag.android

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

internal class ExitInfoCallback(
    private val context: Context,
    private val eventEnhancer: (Event, ApplicationExitInfo) -> Unit,
) : OnSendCallback {

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onSend(event: Event): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val allExitInfo = am.getHistoricalProcessExitReasons(context.packageName, 0, 100)
        allExitInfo.forEach { exitInfo ->
            if (event.session?.id?.let { exitInfo.processStateSummary?.contentEquals(it.toByteArray()) } == true) {
                eventEnhancer
                return@onSend true
            }
        }
        return true
    }
}
