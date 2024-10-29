package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.okhttp.OkHttpDelivery
import java.lang.RuntimeException

internal class OkHttpDeliveryScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.delivery = OkHttpDelivery()
    }

    override fun startScenario() {
        super.startScenario()
        throw RuntimeException("Unhandled Error")
    }
}
