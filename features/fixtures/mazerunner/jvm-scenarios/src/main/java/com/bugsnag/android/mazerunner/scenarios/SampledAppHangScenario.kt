package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.AppHangConfiguration
import com.bugsnag.android.BugsnagAppHangPlugin
import com.bugsnag.android.Configuration
import kotlin.system.exitProcess

private const val SAMPLING_THRESHOLD = 250L
private const val APP_HANG_THRESHOLD = 1000L
private const val APP_HANG_THRESHOLD3 = APP_HANG_THRESHOLD * 3L

class SampledAppHangScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.enabledErrorTypes.anrs = false
        config.addOnSend { event ->
            // drop the Thread.sleep frames off the sampled stacktrace so the scenario
            // can just check which of our functions was calling Thread.sleep
            event.errors.forEach {
                it.stacktrace.removeIf { it.method?.startsWith("java.lang.Thread") == true }
            }
            true
        }
        config.addPlugin(
            BugsnagAppHangPlugin(
                AppHangConfiguration(
                    appHangThresholdMillis = APP_HANG_THRESHOLD,
                    stackSamplingThresholdMillis = SAMPLING_THRESHOLD
                )
            )
        )
    }

    private fun verySlowFunction() {
        Thread.sleep(SAMPLING_THRESHOLD)
        Thread.sleep(SAMPLING_THRESHOLD)
        Thread.sleep(SAMPLING_THRESHOLD)
    }

    override fun startScenario() {
        super.startScenario()

        Handler(Looper.getMainLooper()).postDelayed(
            Runnable {
                verySlowFunction()
                Thread.sleep(APP_HANG_THRESHOLD3)
                exitProcess(0)
            },
            1
        )
    }
}
