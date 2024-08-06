package com.bugsnag.android.mazerunner.scenarios

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Client
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.BugsnagIntentParams
import com.bugsnag.android.mazerunner.MazerunnerHttpClient
import com.bugsnag.android.mazerunner.log
import com.bugsnag.android.multiprocess.MultiProcessService
import com.bugsnag.android.multiprocess.findCurrentProcessName
import com.bugsnag.android.performance.measureSpan
import java.io.File
import kotlin.system.measureNanoTime

private const val RECEIVER_EXPORTED = 2

abstract class Scenario(
    protected val config: Configuration,
    protected val context: Context,
    protected val eventMetadata: String?
) : Application.ActivityLifecycleCallbacks {

    /**
     * Tracks whether the scenario is starting Bugsnag only, or running the scenario.
     */
    protected var startBugsnagOnly: Boolean = false

    internal var mazerunnerHttpClient: MazerunnerHttpClient? = null

    /**
     * Determines what log messages should be intercepted from Bugsnag and sent to Mazerunner
     * using a HTTP requests to the /logs endpoint. This is used to assert that Bugsnag is
     * behaving correctly in situations where sending an error/session payload is not
     * possible.
     */
    open fun getInterceptedLogMessages(): List<String> {
        return emptyList<String>()
    }

    internal inline fun <R> reportDuration(
        tag: String,
        vararg dimensions: Pair<String, String>,
        block: () -> R
    ): R {
        val returnValue: R
        val duration = measureNanoTime {
            returnValue = block()
        }

        Log.i("Metrics", "Reporting duration of $tag to $mazerunnerHttpClient")

        mazerunnerHttpClient?.postMetric(
            "metric.measurement" to tag,
            "duration.nanos" to duration.toString(),
            *dimensions,
            "device.manufacturer" to Build.MANUFACTURER,
            "device.model" to Build.DEVICE,
            "os.sdkLevel" to Build.VERSION.SDK_INT.toString()
        )

        return returnValue
    }

    internal inline fun <R> reportBugsnagStartupDuration(startup: () -> R): R {
        return reportDuration(
            "Bugsnag.start",
            "config.sendLaunchCrashesSynchronously" to config.sendLaunchCrashesSynchronously.toString()
        ) { startup() }
    }

    fun measureBugsnagStartupDuration(context: Context, config: Configuration): Client {
        return measureSpan("Bugsnag Startup") {
            Bugsnag.start(context, config)
        }
    }

    /**
     * Initializes Bugsnag. It is possible to override this method if the scenario requires
     * it - e.g., if the config needs to be loaded from the manifest.
     */
    open fun startBugsnag(startBugsnagOnly: Boolean) {
        this.startBugsnagOnly = startBugsnagOnly
        measureBugsnagStartupDuration(context, config)
    }

    /**
     * Runs code which should result in Bugsnag capturing an error or session.
     */
    open fun startScenario() {
        startBugsnagOnly = false
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
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                log("Received '${MultiProcessService.ACTION_LAUNCHED_MULTI_PROCESS}' broadcast")
                // explicitly post on the main thread to avoid
                // the broadcast receiver wrapping exceptions
                Handler(Looper.getMainLooper()).post {
                    callback()
                }
            }
        }

        @SuppressLint("WrongConstant")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        val intent = Intent(context, MultiProcessService::class.java)
        params.encode(intent)
        log("Starting MultiProcessService")
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

    protected fun errorsDir(): File {
        return File(context.cacheDir, "bugsnag/errors")
    }

    protected fun waitForEventFile() {
        val dir = errorsDir()
        while (dir.listFiles()!!.isEmpty()) {
            dir.listFiles()?.forEach { println(it) }
            Thread.sleep(100)
        }
    }

    protected fun waitForNoEventFiles() {
        val dir = errorsDir()
        while (!dir.listFiles()!!.isEmpty()) {
            dir.listFiles()?.forEach { println(it) }
            Thread.sleep(1000)
        }
    }

    protected fun onAppBackgrounded(listener: () -> Unit) {
        (context.applicationContext as Application).registerComponentCallbacks(
            object : ComponentCallbacks2 {
                override fun onTrimMemory(level: Int) {
                    if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                        Handler(Looper.getMainLooper()).post(listener)
                    }
                }

                override fun onConfigurationChanged(newConfig: android.content.res.Configuration) =
                    Unit

                override fun onLowMemory() = Unit
            }
        )
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    companion object {
        private fun loadClass(className: String): Class<*> {
            try {
                return Class.forName(className)
            } catch (exc: Throwable) {
                throw IllegalStateException(
                    "Failed to load test case class $className. Is it a valid JVM class?",
                    exc
                )
            }
        }

        fun load(
            context: Context,
            config: Configuration,
            eventType: String,
            eventMetaData: String?,
            mazerunnerHttpClient: MazerunnerHttpClient
        ): Scenario {
            log("Loading scenario $eventType with metadata $eventMetaData")
            val clz = loadClass("com.bugsnag.android.mazerunner.scenarios.$eventType")

            try {
                val constructor = clz.constructors[0]
                val scenario = constructor.newInstance(config, context, eventMetaData) as Scenario
                scenario.mazerunnerHttpClient = mazerunnerHttpClient
                return scenario
            } catch (exc: Throwable) {
                throw IllegalStateException(
                    "Failed to construct test case for $eventType. Please check the nested" +
                        " exceptions for more details.",
                    exc
                )
            }
        }
    }
}
