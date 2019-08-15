package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.*

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.content.Context

abstract class Scenario(
    val config: Configuration,
    val context: Context
): Application.ActivityLifecycleCallbacks {

    var eventMetaData: String? = null

    open fun run() {

    }

    /**
     * Sets a NOP implementation for the Session Tracking API, preventing delivery
     */
    fun disableSessionDelivery() {
        val baseDelivery = Bugsnag.getClient().config.delivery
        Bugsnag.getClient().config.delivery = object: Delivery {
            override fun deliver(report: Report, deliveryParams: DeliveryParams): DeliveryStatus {
                return baseDelivery.deliver(report, deliveryParams)
            }

            override fun deliver(payload: SessionTrackingPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                return DeliveryStatus.UNDELIVERED
            }
        }
    }

    /**
     * Sets a NOP implementation for the Error Tracking API, preventing delivery
     */
    fun disableReportDelivery() {
        val baseDelivery = Bugsnag.getClient().config.delivery
        Bugsnag.getClient().config.delivery = object: Delivery {
            override fun deliver(report: Report, deliveryParams: DeliveryParams): DeliveryStatus {
                return DeliveryStatus.UNDELIVERED
            }

            override fun deliver(payload: SessionTrackingPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                return baseDelivery.deliver(payload, deliveryParams)
            }
        }
    }

    fun disableAllDelivery(config: Configuration) {
        config.delivery = object: Delivery {
            override fun deliver(report: Report, deliveryParams: DeliveryParams): DeliveryStatus {
                return DeliveryStatus.UNDELIVERED
            }

            override fun deliver(payload: SessionTrackingPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                return DeliveryStatus.UNDELIVERED
            }
        }
    }

    /**
     * Returns a throwable with the message as the current classname
     */
    fun generateException(): Throwable = RuntimeException(javaClass.simpleName)


    /* Activity lifecycle callback overrides */

    fun registerActivityLifecycleCallbacks() {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {}
    override fun onActivityDestroyed(activity: Activity) {}

}
