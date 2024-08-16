package com.bugsnag.android

import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

internal class AnrPlugin : Plugin {

    internal companion object {

        private const val LOAD_ERR_MSG = "Native library could not be linked. Bugsnag will " +
            "not report ANRs. See https://docs.bugsnag.com/platforms/android/anr-link-errors"
        private const val ANR_ERROR_CLASS = "ANR"
        private const val ANR_ERROR_MSG = "Application did not respond to UI input"

        internal fun doesJavaTraceLeadToNativeTrace(
            javaTrace: Array<StackTraceElement>
        ): Boolean {
            if (javaTrace.isEmpty()) {
                return false
            }
            // The only check that will work across all Android versions is the isNativeMethod call.
            return javaTrace.first().isNativeMethod
        }
    }

    private val libraryLoader = LibraryLoader()
    private val oneTimeSetupPerformed = AtomicBoolean(false)
    private lateinit var client: Client
    private val collector = AnrDetailsCollector()

    private external fun setUnwindFunction(unwindFunction: Long)
    private external fun enableAnrReporting()
    private external fun disableAnrReporting()

    private fun loadClass(clz: String): Class<*>? {
        return try {
            Class.forName(clz)
        } catch (exc: Throwable) {
            null
        }
    }

    override fun load(client: Client) {
        this.client = client

        if (!oneTimeSetupPerformed.getAndSet(true)) {
            performOneTimeSetup(client)
        }
        if (libraryLoader.isLoaded) {
            val mainLooper = Looper.getMainLooper()
            if (Looper.myLooper() == mainLooper) {
                initNativePlugin()
            } else {
                Handler(mainLooper).postAtFrontOfQueue {
                    initNativePlugin()
                }
            }
        } else {
            client.logger.e(LOAD_ERR_MSG)
        }
    }

    private fun initNativePlugin() {
        enableAnrReporting()
        client.logger.i("Initialised ANR Plugin")
    }

    private fun performOneTimeSetup(client: Client) {
        val isLoaded = libraryLoader.loadLibrary("bugsnag-plugin-android-anr", client) {
            val error = it.errors[0]
            it.addMetadata("LinkError", "errorClass", error.errorClass)
            it.addMetadata("LinkError", "errorMessage", error.errorMessage)

            error.errorClass = "AnrLinkError"
            error.errorMessage = LOAD_ERR_MSG
            true
        }

        if (isLoaded) {
            @Suppress("UNCHECKED_CAST")
            val clz = loadClass("com.bugsnag.android.NdkPlugin") as Class<Plugin>?
            if (clz != null) {
                val ndkPlugin = client.getPlugin(clz)
                if (ndkPlugin != null) {
                    val method = ndkPlugin.javaClass.getMethod("getSignalUnwindStackFunction")

                    @Suppress("UNCHECKED_CAST")
                    val function = method.invoke(ndkPlugin) as Long
                    setUnwindFunction(function)
                }
            }
        }
    }

    override fun unload() {
        if (libraryLoader.isLoaded) {
            disableAnrReporting()
        }
    }

    /**
     * Notifies bugsnag that an ANR has occurred, by generating an Error report and populating it
     * with details of the ANR. Intended for internal use only.
     */
    private fun notifyAnrDetected(nativeTrace: List<NativeStackframe>) {
        try {
            if (client.immutableConfig.shouldDiscardError(ANR_ERROR_CLASS)) {
                return
            }
            // generate a full report as soon as possible, then wait for extra process error info
            val stackTrace = Looper.getMainLooper().thread.stackTrace
            val hasNativeComponent = doesJavaTraceLeadToNativeTrace(stackTrace)

            val exc = RuntimeException()
            exc.stackTrace = stackTrace
            val event = NativeInterface.createEvent(
                exc,
                client,
                SeverityReason.newInstance(SeverityReason.REASON_ANR)
            )
            val err = event.errors[0]
            err.errorClass = ANR_ERROR_CLASS
            err.errorMessage = ANR_ERROR_MSG

            // append native stackframes to error/thread stacktrace
            if (hasNativeComponent) {
                // update error stacktrace
                val nativeFrames = nativeTrace.map { Stackframe(it) }
                err.stacktrace.addAll(0, nativeFrames)

                // update thread stacktrace
                val errThread = event.threads.find(Thread::getErrorReportingThread)
                errThread?.stacktrace?.addAll(0, nativeFrames)
            }

            // wait and poll for error info to be collected. this occurs just before the ANR dialog
            // is displayed
            collector.collectAnrErrorDetails(client, event)
        } catch (exception: Exception) {
            client.logger.e("Internal error reporting ANR", exception)
        }
    }
}
