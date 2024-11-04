package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.StrictMode
import com.bugsnag.android.Configuration
import com.bugsnag.android.okhttp.OkHttpDelivery
import java.lang.RuntimeException

internal class OkHttpDeliveryScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        // there is a StrictMode violation within the OkHttp version we use, so we turn off
        // StrictMode for this scenario
        // StrictMode policy violation: android.os.strictmode.NonSdkApiUsedViolation: Lcom/android/org/conscrypt/OpenSSLSocketImpl;->setUseSessionTickets(Z)V
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX)
        StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX)

        config.delivery = OkHttpDelivery()
    }

    override fun startScenario() {
        super.startScenario()
        throw RuntimeException("Unhandled Error")
    }
}
