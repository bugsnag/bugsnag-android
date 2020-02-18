package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import com.bugsnag.android.Bugsnag

import com.bugsnag.android.Configuration
import com.bugsnag.android.Delivery
import com.bugsnag.android.DeliveryParams
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Session
import com.bugsnag.android.createDefaultDelivery
import com.bugsnag.android.flushAllSessions
import java.io.File

internal class DeletedSessionScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {

    init {
        config.autoTrackSessions = false

        if (context is Activity) {
            eventMetaData = context.intent.getStringExtra("EVENT_METADATA")

            if (eventMetaData != "non-crashy") {
                disableAllDelivery(config)
            } else {
                val baseDelivery = createDefaultDelivery()
                val errDir = File(context.cacheDir, "bugsnag-sessions")

                config.delivery = object: Delivery {
                    override fun deliver(payload: Session,
                                         deliveryParams: DeliveryParams): DeliveryStatus {
                        // delete files before they can be delivered
                        val files = errDir.listFiles()
                        files.forEach {
                            it.delete()
                        }
                        return baseDelivery.deliver(payload, deliveryParams)
                    }

                    override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                        return baseDelivery.deliver(payload, deliveryParams)
                    }
                }
            }
        }
    }

    override fun run() {
        super.run()

        if (eventMetaData != "non-crashy") {
            Bugsnag.startSession()
        }

        val thread = HandlerThread("HandlerThread")
        thread.start()

        Handler(thread.looper).post(Runnable {
            flushAllSessions()
        })
    }
}
