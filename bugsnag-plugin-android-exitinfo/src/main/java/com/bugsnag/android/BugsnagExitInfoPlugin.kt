package com.bugsnag.android

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.R)
class BugsnagExitInfoPlugin @JvmOverloads constructor(

    /**
     * Whether to add the list of open FDs to correlated reports
     */
    private val listOpenFds: Boolean = true,

    /**
     * Whether to report stored logcat messages metadata
     */
    private val includeLogcat: Boolean = false,

    /**
     * Turn of event correlation based on the processStateSummary field, this can
     * set to `true` if the field is required by the app
     */
    private val disableProcessStateSummaryOverride: Boolean = false

) : Plugin {

    private var exitInfoCallback: ExitInfoCallback? = null

    override fun load(client: Client) {
        if (!disableProcessStateSummaryOverride) {
            client.addOnSession(
                OnSessionCallback { session: Session ->
                    val am =
                        client.appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    am.setProcessStateSummary(session.id.toByteArray())
                    return@OnSessionCallback true
                }
            )
        }

        val exitInfoPluginStore = ExitInfoPluginStore(client.immutableConfig)
        val oldPid = exitInfoPluginStore.load()
        exitInfoPluginStore.persist(android.os.Process.myPid())

        exitInfoCallback = ExitInfoCallback(
            client.appContext,
            oldPid,
            TombstoneEventEnhancer(client.logger, listOpenFds, includeLogcat),
            TraceEventEnhancer(client.logger, client.immutableConfig.projectPackages)
        )
        client.addOnSend(exitInfoCallback)
    }

    override fun unload() {
    }
}
