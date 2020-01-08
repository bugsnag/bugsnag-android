package com.bugsnag.android

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

    private external fun enableAnrReporting()
    private external fun disableAnrReporting()

    override fun loadPlugin(client: Client) {
        enableAnrReporting()
        Logger.info("Initialised ANR Plugin")
        this.client = client
    }

    override fun unloadPlugin() = disableAnrReporting()

    /**
     * Notifies bugsnag that an ANR has occurred, by generating an Error report and populating it
     * with details of the ANR. Intended for internal use only.
     */
    private fun notifyAnrDetected() {
        val thread = Looper.getMainLooper().thread

        // generate a full report as soon as possible, then wait for extra process error info
        val errMsg = "Application did not respond to UI input"
        val exc = BugsnagException("ANR", errMsg, thread.stackTrace)
        val error = Error.Builder(client.config, exc, client.sessionTracker, thread, true)
            .severity(Severity.ERROR)
            .severityReasonType(HandledState.REASON_ANR)
            .build()

        // wait and poll for error info to be collected. this occurs just before the ANR dialog
        // is displayed
        collector.collectAnrErrorDetails(client, error)
    }

}
