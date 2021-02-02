package com.bugsnag.android

import android.os.Handler
import android.os.Looper

internal class AnrPlugin : Plugin {

    internal companion object {
        private const val LOAD_ERR_MSG = "Native library could not be linked. Bugsnag will " +
            "not report ANRs. See https://docs.bugsnag.com/platforms/android/anr-link-errors"

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

    private val loader = LibraryLoader()
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
        val loaded = loader.loadLibrary("bugsnag-plugin-android-anr", client) {
            val error = it.errors[0]
            error.errorClass = "AnrLinkError"
            error.errorMessage = LOAD_ERR_MSG
            true
        }

        if (loaded) {
            @Suppress("UNCHECKED_CAST")
            val clz = loadClass("com.bugsnag.android.NdkPlugin") as Class<Plugin>?
            if (clz != null) {
                val ndkPlugin = client.getPlugin(clz)
                if (ndkPlugin != null) {
                    val method = ndkPlugin.javaClass.getMethod("getUnwindStackFunction")
                    @Suppress("UNCHECKED_CAST")
                    val function = method.invoke(ndkPlugin) as Long
                    setUnwindFunction(function)
                }
            }

            // this must be run from the main thread as the SIGQUIT is sent to the main thread,
            // and if the handler is installed on a background thread instead we receive no signal
            Handler(Looper.getMainLooper()).post(
                Runnable {
                    this.client = client
                    enableAnrReporting()
                    client.logger.i("Initialised ANR Plugin")
                }
            )
        } else {
            client.logger.e(LOAD_ERR_MSG)
        }
    }

    override fun unload() = disableAnrReporting()

    /**
     * Notifies bugsnag that an ANR has occurred, by generating an Error report and populating it
     * with details of the ANR. Intended for internal use only.
     */
    private fun notifyAnrDetected(nativeTrace: List<NativeStackframe>) {
        try {
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
            err.errorClass = "ANR"
            err.errorMessage = "Application did not respond to UI input"

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
