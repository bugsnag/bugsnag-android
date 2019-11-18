package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import com.bugsnag.android.Bugsnag

import com.bugsnag.android.Configuration
import com.bugsnag.android.Delivery
import com.bugsnag.android.DeliveryParams
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.Report
import com.bugsnag.android.SessionPayload
import java.io.File

internal class DeletedReportScenario(config: Configuration,
                                     context: Context) : Scenario(config, context) {

    init {
        config.setAutoTrackSessions(false)

        if (context is Activity) {
            eventMetaData = context.intent.getStringExtra("eventMetaData")

            if (eventMetaData != "non-crashy") {
                disableAllDelivery(config)
            } else {
                val ctor = Class.forName("com.bugsnag.android.DefaultDelivery").declaredConstructors[0]
                ctor.isAccessible = true
                val baseDelivery = ctor.newInstance(null) as Delivery
                val errDir = File(context.cacheDir, "bugsnag-errors")

                config.delivery = object: Delivery {
                    override fun deliver(payload: SessionPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                        return baseDelivery.deliver(payload, deliveryParams)
                    }

                    override fun deliver(report: Report, deliveryParams: DeliveryParams): DeliveryStatus {
                        // delete files before they can be delivered
                        val files = errDir.listFiles()
                        files.forEach {
                            it.delete()
                        }
                        return baseDelivery.deliver(report, deliveryParams)
                    }
                }
            }
        }
    }

    override fun run() {
        super.run()

        if (eventMetaData != "non-crashy") {
            Bugsnag.notify(java.lang.RuntimeException("Whoops"))
        }
    }
}
