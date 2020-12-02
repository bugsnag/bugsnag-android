package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.bugsnag.android.*
import com.bugsnag.android.JavaHooks.generateDelivery
import com.bugsnag.android.flushAllSessions
import java.lang.Thread

/**
 * Sends an exception after pausing the session
 */
internal class ManualSessionSmokeScenario(config: Configuration,
                                          context: Context) : Scenario(config, context) {

    init {
        config.autoTrackSessions = false
        val baseDelivery = createDefaultDelivery()

        class InterceptingDelivery(private val baseDelivery: Delivery,
                                   private val callback: (state: Int) -> Unit): Delivery {

            var state = 0

            override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                val response = baseDelivery.deliver(payload, deliveryParams)
                Log.d("Bugsnag testing", "Event $state")
                callback(state)
                state++
                return response
            }

            override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
                val response =  baseDelivery.deliver(payload, deliveryParams)
                Log.d("Bugsnag testing", "Session $state")
                callback(state)
                state++
                return response
            }
        }

        config.delivery = InterceptingDelivery(baseDelivery) {
            when (it) {
                0 -> Bugsnag.notify(generateException())
                1 -> {
                    Bugsnag.pauseSession()
                    Bugsnag.notify(generateException())
                }
                2 -> {
                    Bugsnag.resumeSession()
                    throw generateException()
                }
            }
        }

    }

    override fun run() {
        super.run()
        Bugsnag.setUser("123", "ABC.CBA.CA", "ManualSessionSmokeScenario")
        Bugsnag.startSession()
    }
}
