package com.bugsnag.android

import com.bugsnag.android.NativeInterface.MessageType.UPDATE_NOTIFY_RELEASE_STAGES
import com.bugsnag.android.NativeInterface.MessageType.UPDATE_RELEASE_STAGE

import java.util.Observable
import java.util.Observer

internal class ClientConfigObserver(
    private val client: Client,
    private val config: Configuration
) : Observer {

    override fun update(o: Observable?, arg: Any?) {
        val msg = arg as NativeInterface.Message

        when {
            msg.type == UPDATE_NOTIFY_RELEASE_STAGES -> handleNotifyReleaseStages()
            msg.type == UPDATE_RELEASE_STAGE -> handleNotifyReleaseStages()
        }
    }

    private fun handleNotifyReleaseStages() {
        if (config.shouldNotifyForReleaseStage(config.releaseStage)) {
            if (config.detectAnrs) {
                client.enableAnrReporting()
            }
            if (config.detectNdkCrashes) {
                client.enableNdkCrashReporting()
            }
        } else {
            client.disableAnrReporting()
            client.disableNdkCrashReporting()
        }
    }
}
