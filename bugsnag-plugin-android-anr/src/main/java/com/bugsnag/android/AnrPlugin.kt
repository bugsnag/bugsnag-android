package com.bugsnag.android

import java.nio.ByteBuffer

internal class AnrPlugin : BugsnagPlugin {

    companion object {
        private const val INFO_POLL_THRESHOLD_MS: Long = 2500
    }

    private external fun installAnrDetection(sentinelBuffer: ByteBuffer)

    val collector = AnrDetailsCollector()

    override fun initialisePlugin(client: Client) {
        System.loadLibrary("bugsnag-plugin-android-anr")
        val delegate: (Thread) -> Unit = { handleAnr(it, client) }
        val monitor = AppNotRespondingMonitor(delegate)
        monitor.start()
        installAnrDetection(monitor.sentinelBuffer)
        Logger.info("Initialised ANR Plugin")
    }

    private fun handleAnr(thread: Thread, client: Client) {
        // generate a full report as soon as possible, then wait for extra process error info
        val errMsg = "Application did not respond to UI input"
        val exc = BugsnagException("ANR", errMsg, thread.stackTrace)
        val error = Error.Builder(client.config, exc, client.sessionTracker, thread, true)
            .severity(Severity.ERROR)
            .severityReasonType(HandledState.REASON_ANR)
            .build()

        // wait for the ANR dialog to show, reporting once the information is available
        collectAnrErrorDetails(client, error)
    }

    private fun collectAnrErrorDetails(client: Client, error: Error) {
        // TODO add timeouts, and better way of posting async runnable

        Async.run(object : Runnable {
            override fun run() {
                try {
                    Thread.sleep(INFO_POLL_THRESHOLD_MS)
                    val anrDetails = collector.collectAnrDetails(client.appContext)

                    if (anrDetails == null) {
                        Logger.warn("Looping in attempt to capture ANR details")
                        Async.run(this)
                    } else {
                        Logger.warn("Captured ANR details: $anrDetails")

                        // TODO determine how to populate error report
                        val metaData = error.metaData
                        metaData.addToTab("ANR", "Message", anrDetails.shortMsg)
                        metaData.addToTab("ANR", "Component", anrDetails.tag)
                        metaData.addToTab("ANR", "Details", anrDetails.longMsg)

                        client.notify(error, DeliveryStyle.ASYNC_WITH_CACHE, null)
                    }
                } catch (exc: InterruptedException) {
                }
            }
        })
    }

}
