package com.bugsnag.android

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.R)
class BugsnagExitInfoPlugin @JvmOverloads constructor(
    configuration: ExitInfoPluginConfiguration = ExitInfoPluginConfiguration()
) : Plugin {

    private val configuration = configuration.copy()
    private lateinit var internalHooks: InternalHooks

    override fun load(client: Client) {
        internalHooks = InternalHooks()
        if (!configuration.disableProcessStateSummaryOverride) {
            client.addOnSession(
                OnSessionCallback { session: Session ->
                    val am = client.appContext.safeGetActivityManager()
                    am?.setProcessStateSummary(session.id.toByteArray())
                    return@OnSessionCallback true
                }
            )
        }

        val exitInfoPluginStore =
            ExitInfoPluginStore(client.immutableConfig)
        val (oldPid, exitInfoKeys) = exitInfoPluginStore.load()
        exitInfoPluginStore.persist(android.os.Process.myPid(), exitInfoKeys)

        val exitInfoCallback = ExitInfoCallback(
            client.appContext,
            oldPid,
            TombstoneEventEnhancer(
                client.logger,
                configuration.listOpenFds,
                configuration.includeLogcat
            ),
            TraceEventEnhancer(
                client.logger,
                client.immutableConfig.projectPackages
            ),
            exitInfoPluginStore
        )

        client.addOnSend(exitInfoCallback)
    }

    override fun unload() = Unit

    private fun Context.safeGetActivityManager(): ActivityManager? = try {
        getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    } catch (e: Exception) {
        null
    }
}
