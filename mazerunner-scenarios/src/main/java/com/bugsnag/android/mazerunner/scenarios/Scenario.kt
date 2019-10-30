package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.content.Context

import com.bugsnag.android.*

abstract class Scenario(
    protected val config: Configuration,
    protected val context: Context
): Application.ActivityLifecycleCallbacks {

    var eventMetadata: String? = null

    open fun run() {

    }

    /**
     * Sets a NOP implementation for the Session Tracking API, preventing delivery
     */
    protected fun disableSessionDelivery(config: Configuration) {
        val baseDelivery = createDefaultDelivery()
        config.delivery = object: Delivery {
            override fun deliver(report: Report, deliveryParams: DeliveryParams): DeliveryStatus {
                return baseDelivery.deliver(report, deliveryParams)
            }

            override fun deliver(payload: SessionPayload, deliveryParams: DeliveryParams): DeliveryStatus {
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
            override fun deliver(report: Report, deliveryParams: DeliveryParams): DeliveryStatus {
                return DeliveryStatus.UNDELIVERED
            }

            override fun deliver(payload: SessionPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                return baseDelivery.deliver(payload, deliveryParams)
            }
        }
    }

    protected fun disableAllDelivery(config: Configuration) {
        config.delivery = object: Delivery {
            override fun deliver(report: Report, deliveryParams: DeliveryParams): DeliveryStatus {
                return DeliveryStatus.UNDELIVERED
            }

            override fun deliver(payload: SessionPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                return DeliveryStatus.UNDELIVERED
            }
        }
    }

    internal fun createDefaultDelivery(): Delivery { // use reflection as DefaultDelivery is internal
        val clz = Class.forName("com.bugsnag.android.DefaultDelivery")
        return clz.constructors[0].newInstance(null, object: Logger {
            override fun e(msg: String) = Unit
            override fun e(msg: String, throwable: Throwable) = Unit
            override fun w(msg: String) = Unit
            override fun w(msg: String, throwable: Throwable) = Unit
            override fun i(msg: String) = Unit
            override fun i(msg: String, throwable: Throwable) = Unit
            override fun d(msg: String) = Unit
            override fun d(msg: String, throwable: Throwable) = Unit
        }) as Delivery
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
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {}
    override fun onActivityDestroyed(activity: Activity) {}

}
