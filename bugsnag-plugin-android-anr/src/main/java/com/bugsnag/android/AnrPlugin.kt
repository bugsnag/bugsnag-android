package com.bugsnag.android

import android.os.Handler
import android.os.Looper

internal class AnrPlugin : Plugin {

    private companion object {
        private const val LOAD_ERR_MSG = "Native library could not be linked. Bugsnag will " +
            "not report ANRs. See https://docs.bugsnag.com/platforms/android/anr-link-errors"
    }

    private val loader = LibraryLoader()
    private lateinit var client: Client
    private val collector = AnrDetailsCollector()

    private external fun enableAnrReporting(callPreviousSigquitHandler: Boolean)
    private external fun disableAnrReporting()

    override fun load(client: Client) {
        val loaded = loader.loadLibrary("bugsnag-plugin-android-anr", client) {
            val error = it.errors[0]
            error.errorClass = "AnrLinkError"
            error.errorMessage = LOAD_ERR_MSG
            true
        }

        if (loaded) {
            // this must be run from the main thread as the SIGQUIT is sent to the main thread,
            // and if the handler is installed on a background thread instead we receive no signal
            Handler(Looper.getMainLooper()).post(
                Runnable {
                    this.client = client
                    enableAnrReporting(true)
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
    @Suppress("UNUSED_PARAMETER")
    private fun notifyAnrDetected(info: Long, userContext: Long) {
        val thread = Looper.getMainLooper().thread

        // generate a full report as soon as possible, then wait for extra process error info
        val exc = RuntimeException()
        exc.stackTrace = thread.stackTrace

        @Suppress("UNCHECKED_CAST")
        val clz = Class.forName("com.bugsnag.android.NdkPlugin") as Class<Plugin>
        val ndkPlugin = client.getPlugin(clz)
        if (ndkPlugin != null) {
            val method = ndkPlugin.javaClass.getMethod("getSignalStackTrace", Long::class.java, Long::class.java)
            @Suppress("UNCHECKED_CAST")
            val list = method.invoke(ndkPlugin, info, userContext) as List<Stackframe>

            for (frame in list) client.logger.e(
                "### TODO: ANR TRACE: " + frame.file + ": " + frame.lineNumber + ": " + frame.method
            )
        }

        val event = NativeInterface.createEvent(
            exc,
            client,
            HandledState.newInstance(HandledState.REASON_ANR)
        )
        val err = event.errors[0]
        err.errorClass = "ANR"
        err.errorMessage = "Application did not respond to UI input"

        // wait and poll for error info to be collected. this occurs just before the ANR dialog
        // is displayed
        collector.collectAnrErrorDetails(client, event)
    }
}
