package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.AppHangConfiguration
import com.bugsnag.android.BugsnagAppHangPlugin
import com.bugsnag.android.Configuration
import kotlin.system.exitProcess

private const val HEADROOM = 10L
private const val NEAR_HANG_THRESHOLD = 800L
private const val APP_HANG_THRESHOLD = 1000L
private const val APP_HANG_THRESHOLD3 = APP_HANG_THRESHOLD * 3L

class AppHangPluginScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {
    init {
        config.enabledErrorTypes.anrs = false
        config.addPlugin(
            BugsnagAppHangPlugin(
                AppHangConfiguration(
                    appHangThresholdMillis = APP_HANG_THRESHOLD,
                    nearHangThresholdMillis = NEAR_HANG_THRESHOLD
                )
            )
        )
    }

    override fun startScenario() {
        super.startScenario()

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(
            Runnable {
                Thread.sleep(NEAR_HANG_THRESHOLD + HEADROOM)

                handler.postDelayed(
                    Runnable {
                        Thread.sleep(APP_HANG_THRESHOLD3)
                        exitProcess(0)
                    },
                    HEADROOM
                )
            },
            HEADROOM
        )
    }
}
