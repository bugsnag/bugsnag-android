package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.Delivery
import com.bugsnag.android.DeliveryParams
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Session
import com.bugsnag.android.createDefaultDelivery

abstract class Scenario(
    protected val config: Configuration,
    protected val context: Context
): Application.ActivityLifecycleCallbacks {

    var eventMetaData: String? = null

    open fun run() {

    }

    /**
     * Sets a NOP implementation for the Session Tracking API, preventing delivery
     */
    protected fun disableSessionDelivery(config: Configuration) {
        val baseDelivery = createDefaultDelivery()
        config.delivery = object: Delivery {
            override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                return baseDelivery.deliver(payload, deliveryParams)
            }

            override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
                return DeliveryStatus.UNDELIVERED
            }
        }
    }

    /**
     * Sets a NOP implementation for the Error Tracking API, preventing delivery
     */
    protected fun disableReportDelivery(config: Configuration) {
        val baseDelivery = createDefaultDelivery()
        config.delivery = object: Delivery {
            override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                return DeliveryStatus.UNDELIVERED
            }

            override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
                return baseDelivery.deliver(payload, deliveryParams)
            }
        }
    }

    protected fun disableAllDelivery(config: Configuration) {
        config.delivery = object: Delivery {
            override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                return DeliveryStatus.UNDELIVERED
            }

            override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
                return DeliveryStatus.UNDELIVERED
            }
        }
    }

    /**
     * Returns a throwable with the message as the current classname
     */
    protected fun generateException(): Throwable = RuntimeException(javaClass.simpleName)


    /* Activity lifecycle callback overrides */

    protected fun registerActivityLifecycleCallbacks() {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

}
