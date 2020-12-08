package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.content.Context
import android.content.Intent

import com.bugsnag.android.*
import com.bugsnag.android.mazerunner.log
import com.bugsnag.android.mazerunner.multiprocess.MultiProcessService
import com.bugsnag.android.mazerunner.multiprocess.findCurrentProcessName

abstract class Scenario(
    protected val config: Configuration,
    protected val context: Context,
    protected val eventMetadata: String?
): Application.ActivityLifecycleCallbacks {

    /**
     * Initializes Bugsnag. It is possible to override this method if the scenario requires
     * it - e.g., if the config needs to be loaded from the manifest.
     */
    open fun startBugsnag() {
        Bugsnag.start(context, config)
    }

    /**
     * Runs code which should result in Bugsnag capturing an error or session.
     */
    open fun startScenario() {

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

    /**
     * Launches the [MultiProcessService] which runs in a different process. The [Intent]
     * determines what scenario the service executes.
     */
    protected fun launchMultiProcessService(cb: (Intent) -> Unit) {
        val intent = Intent(context, MultiProcessService::class.java).apply(cb)
        context.startService(intent)
    }

    /**
     * Returns true if the scenario is running from the background service
     */
    protected fun isRunningFromBackgroundService() = findCurrentProcessName() ==
            "com.example.bugsnag.android.mazerunner.multiprocess"

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

    companion object {
        fun load(
            context: Context,
            config: Configuration,
            eventType: String,
            eventMetaData: String?
        ): Scenario {
            try {
                log("Loading scenario $eventType with metadata $eventMetaData")
                val clz = Class.forName("com.bugsnag.android.mazerunner.scenarios.$eventType")
                val constructor = clz.constructors[0]
                return constructor.newInstance(config, context, eventMetaData) as Scenario
            } catch (exc: Throwable) {
                throw IllegalStateException(
                    "Failed to instantiate test case for $eventType. Is it a valid JVM class?",
                    exc
                )
            }
        }
    }
}
