package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.storage.StorageManager
import android.util.Log

import com.bugsnag.android.*
import com.bugsnag.android.Configuration
import java.io.File

internal class DeletedSessionScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {

    init {
        config.setAutoCaptureSessions(false)

        if (context is Activity) {
            eventMetaData = context.intent.getStringExtra("EVENT_METADATA")

            if (eventMetaData != "non-crashy") {
                disableAllDelivery(config)
            } else {
                val ctor = Class.forName("com.bugsnag.android.DefaultDelivery").declaredConstructors[0]
                ctor.isAccessible = true
                val baseDelivery = ctor.newInstance(null) as Delivery
                val errDir = File(context.cacheDir, "bugsnag-sessions")

                config.delivery = object: Delivery {
                    override fun deliver(payload: SessionPayload, config: Configuration) {
                        // delete files before they can be delivered
                        val files = errDir.listFiles()
                        files.forEach {
                            Log.d("Bugsnag", "Deleting file: ${it.delete()}")
                        }

                        Log.d("Bugsnag", "Files available " + errDir.listFiles()[0].exists())
                        baseDelivery.deliver(payload, config)
                    }

                    override fun deliver(report: Report, config: Configuration) {
                        baseDelivery.deliver(report, config)
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

        Handler(thread.looper).post {
            flushAllSessions()
        }
    }
}
