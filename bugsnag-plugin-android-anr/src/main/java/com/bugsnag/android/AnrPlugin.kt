package com.bugsnag.android

import android.os.Handler
import android.os.Looper

internal class AnrPlugin : BugsnagPlugin {

    companion object {
        init {
            System.loadLibrary("bugsnag-plugin-android-anr")
        }
    }

    override var loaded = false
    private lateinit var client: Client
    private val collector = AnrDetailsCollector()

    private external fun enableAnrReporting(callPreviousSigquitHandler: Boolean)
    private external fun disableAnrReporting()

    override fun loadPlugin(client: Client) {
        // this must be run from the main thread as the SIGQUIT is sent to the main thread,
        // and if the handler is installed on a background thread instead we receive no signal
        Handler(Looper.getMainLooper()).post(Runnable {
            this.client = client
            enableAnrReporting(true)
            client.logger.w("Initialised ANR Plugin")
        })
    }

    override fun unloadPlugin() = disableAnrReporting()

    /**
     * Notifies bugsnag that an ANR has occurred, by generating an Error report and populating it
     * with details of the ANR. Intended for internal use only.
     */
    private fun notifyAnrDetected() {
        val thread = Looper.getMainLooper().thread

        // generate a full report as soon as possible, then wait for extra process error info
        val exc = RuntimeException()
        exc.stackTrace = thread.stackTrace

        val event = BugsnagPluginInterface.createEvent(exc, client, HandledState.REASON_ANR)
        val err = event.errors[0]
        err.errorClass = "ANR"
        err.errorMessage = "Application did not respond to UI input"

        // wait and poll for error info to be collected. this occurs just before the ANR dialog
        // is displayed
        collector.collectAnrErrorDetails(client, event)
    }

}
