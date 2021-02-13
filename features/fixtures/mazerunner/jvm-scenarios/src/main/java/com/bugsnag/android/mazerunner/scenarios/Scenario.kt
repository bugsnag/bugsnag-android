package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.Delivery
import com.bugsnag.android.DeliveryParams
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Session
import com.bugsnag.android.createDefaultDelivery
import com.bugsnag.android.mazerunner.BugsnagIntentParams
import com.bugsnag.android.mazerunner.getZeroEventsLogMessages
import com.bugsnag.android.mazerunner.log
import com.bugsnag.android.mazerunner.multiprocess.MultiProcessService
import com.bugsnag.android.mazerunner.multiprocess.findCurrentProcessName

abstract class Scenario(
    protected val config: Configuration,
    protected val context: Context,
    protected val eventMetadata: String?
) : Application.ActivityLifecycleCallbacks {

    /**
     * Tracks whether the scenario is starting Bugsnag only, or running the scenario.
     */
    protected var startBugsnagOnly: Boolean = false

    /**
     * Determines what log messages should be intercepted from Bugsnag and sent to Mazerunner
     * using a HTTP requests to the /logs endpoint. This is used to assert that Bugsnag is
     * behaving correctly in situations where sending an error/session payload is not
     * possible.
     */
    open fun getInterceptedLogMessages() = getZeroEventsLogMessages(startBugsnagOnly)

    /**
     * Initializes Bugsnag. It is possible to override this method if the scenario requires
     * it - e.g., if the config needs to be loaded from the manifest.
     */
    open fun startBugsnag(startBugsnagOnly: Boolean) {
        log("startBugsnag called with: " + startBugsnagOnly)
        this.startBugsnagOnly = startBugsnagOnly
        Bugsnag.start(context, config)
    }

    /**
     * Runs code which should result in Bugsnag capturing an error or session.
     */
    open fun startScenario() {
        log("startScenario startBugsnagOnly now false")
        startBugsnagOnly = false
    }

    /**
     * Sets a NOP implementation for the Session Tracking API, preventing delivery
     */
    protected fun disableSessionDelivery(config: Configuration) {
        val baseDelivery = createDefaultDelivery()
        config.delivery = object : Delivery {
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
        config.delivery = object : Delivery {
            override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                return DeliveryStatus.UNDELIVERED
            }

            override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
                return baseDelivery.deliver(payload, deliveryParams)
            }
        }
    }

    protected fun disableAllDelivery(config: Configuration) {
        config.delivery = object : Delivery {
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
     * determines what scenario the service executes. The callback is executed once the process
     * has started.
     */
    protected fun launchMultiProcessService(
        params: BugsnagIntentParams,
        callback: () -> Unit = {}
    ) {
        val filter = IntentFilter(MultiProcessService.ACTION_LAUNCHED_MULTI_PROCESS)
        context.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {

                    // explicitly post on the main thread to avoid
                    // the broadcast receiver wrapping exceptions
                    Handler(Looper.getMainLooper()).post {
                        callback()
                    }
                }
            },
            filter
        )

        val intent = Intent(context, MultiProcessService::class.java)
        params.encode(intent)
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

    protected fun runOnBgThread(block: () -> Unit) {
        val handlerThread = HandlerThread("bg-thread")
        handlerThread.start()
        Handler(handlerThread.looper).post(block)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
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
