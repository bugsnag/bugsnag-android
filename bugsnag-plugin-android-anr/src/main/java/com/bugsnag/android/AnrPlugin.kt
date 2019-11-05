package com.bugsnag.android

import java.nio.ByteBuffer

internal class AnrPlugin : BugsnagPlugin {

    companion object {
        init {
            System.loadLibrary("bugsnag-plugin-android-anr")
        }
    }

    override var loaded = false

    private external fun enableAnrReporting(sentinelBuffer: ByteBuffer)
    private external fun disableAnrReporting()

    private val collector = AnrDetailsCollector()
    private var monitor: AppNotRespondingMonitor? = null

    override fun loadPlugin(client: Client) {
        if (monitor == null) {
            val delegate: (Thread) -> Unit = { handleAnr(it, client) }
            monitor = AppNotRespondingMonitor(delegate)
            monitor?.start()
        }
        if (monitor != null) {
            enableAnrReporting(monitor!!.sentinelBuffer)
            Logger.info("Initialised ANR Plugin")
        }
    }

    override fun unloadPlugin() = disableAnrReporting()

    private fun handleAnr(thread: Thread, client: Client) {
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
