package com.bugsnag.android

import java.lang.Thread
import java.nio.ByteBuffer

internal class AnrPlugin : BugsnagPlugin {

    private external fun installAnrDetection(sentinelBuffer: ByteBuffer)

    private val collector = AnrDetailsCollector()

    override fun initialisePlugin(client: Client) {
        System.loadLibrary("bugsnag-plugin-android-anr")
        val delegate: (Thread) -> Unit = { handleAnr(it, client) }
        val monitor = AppNotRespondingMonitor(delegate)
        monitor.start()
        installAnrDetection(monitor.sentinelBuffer)
    }

    private fun handleAnr(thread: Thread, client: Client) {
        // generate a full report as soon as possible, then wait for extra process error info
        val exc = RuntimeException()
        exc.stackTrace = thread.stackTrace

        val event = BugsnagPluginInterface.createAnrEvent(exc, client)
        val err = event.errors[0]
        err.errorClass = "ANR"
        err.errorMessage = "Application did not respond to UI input"

        // wait and poll for error info to be collected. this occurs just before the ANR dialog
        // is displayed
        collector.collectAnrErrorDetails(client, event)
    }
}
