package com.bugsnag.android

import java.nio.ByteBuffer

internal class AnrPlugin : BugsnagPlugin {

    private external fun installAnrDetection(sentinelBuffer: ByteBuffer)

    override fun initialisePlugin(client: Client) {
        System.loadLibrary("bugsnag-android-anr")
        val delegate: (Thread) -> Unit = { handleAnr(it, client) }
        val monitor = AppNotRespondingMonitor(delegate)
        monitor.start()
        installAnrDetection(monitor.sentinelBuffer)
        Logger.info("Initialised ANR Plugin")
    }

    private fun handleAnr(thread: Thread, client: Client) {
        val errMsg = "Application did not respond to UI input"
        val exc = BugsnagException("ANR", errMsg, thread.stackTrace)

        client.cacheAndNotify(
            exc, Severity.ERROR, MetaData(),
            HandledState.REASON_ANR, null, thread
        )
    }
}
