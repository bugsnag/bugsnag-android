package com.bugsnag.android.mazerunner.scenarios

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import com.bugsnag.android.BugsnagVmViolationListener
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.getZeroEventsLogMessages
import java.io.File

/**
 * Generates a strictmode exception caused by exposing a file URI
 */
internal class StrictModeFileUriExposeScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    @SuppressLint("SdCardPath")
    override fun startScenario() {
        super.startScenario()
        setupBugsnagStrictModeDetection()

        // expose a file URI to another app, triggering a StrictMode violation.
        // https://developer.android.com/reference/android/os/StrictMode.VmPolicy.Builder#detectFileUriExposure()
        val intents = arrayOf(
            Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val fileUri = Uri.fromFile(File("/sdcard/bus.jpg"))
                setDataAndType(fileUri, "image/jpeg")
            },
            Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val fileUri = Uri.fromFile(File("/sdcard/siren.wav"))
                setDataAndType(fileUri, "audio/x-wav")
            },
            Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val fileUri = Uri.fromFile(File("/sdcard/siren.webm"))
                setDataAndType(fileUri, "video/webm; codecs=\"vp8, vorbis\"")
            },
            Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val fileUri = Uri.fromFile(File("/sdcard/bus.pdf"))
                setDataAndType(fileUri, "application/pdf")
            }
        )

        val pm = context.packageManager
        val intent = intents.firstOrNull { pm.queryIntentActivities(it, 0).isNotEmpty() }

        context.startActivity(intent)
    }

    private fun setupBugsnagStrictModeDetection() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return
        }
        val policy = VmPolicy.Builder().detectFileUriExposure() // raises SIGKILL on Android <9

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            policy.penaltyDeath()
        } else {
            val listener = BugsnagVmViolationListener()
            policy.penaltyListener(context.mainExecutor, listener)
        }
        StrictMode.setVmPolicy(policy.build())
    }

    override fun getInterceptedLogMessages(): List<String> {
        return getZeroEventsLogMessages(startBugsnagOnly)
    }
}
