package com.bugsnag.android

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.R)
class BugsnagExitInfoPlugin @JvmOverloads constructor(
    configuration: ExitInfoPluginConfiguration = ExitInfoPluginConfiguration()
) : Plugin {

    private val configuration = configuration.copy()

    @SuppressLint("VisibleForTests")
    override fun load(client: Client) {
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
        val exitInfoCallback = createExitInfoCallback(client, oldPid, exitInfoPluginStore)
        InternalHooks.setEventStoreEmptyCallback(client) {
            synthesizeNewEvents(client, exitInfoPluginStore)
        }
        client.addOnSend(exitInfoCallback)
    }

    private fun createExitInfoCallback(
        client: Client,
        oldPid: Int?,
        exitInfoPluginStore: ExitInfoPluginStore
    ): ExitInfoCallback {
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
        return exitInfoCallback
    }

    private fun synthesizeNewEvents(
        client: Client,
        exitInfoPluginStore: ExitInfoPluginStore
    ) {
        val eventSynthesizer = EventSynthesizer(
            TraceEventEnhancer(
                client.logger,
                client.immutableConfig.projectPackages
            ),
            exitInfoPluginStore
        )
        val context = client.appContext
        val am: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val allExitInfo: List<ApplicationExitInfo> =
            am.getHistoricalProcessExitReasons(context.packageName, 0, 100)
        allExitInfo.forEach {
            val newEvent = eventSynthesizer.createEventWithAnrExitInfo(it)
            if (newEvent != null) {
                InternalHooks.deliver(client, newEvent)
            }
        }
    }

    override fun unload() = Unit

    private fun Context.safeGetActivityManager(): ActivityManager? = try {
        getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    } catch (e: Exception) {
        null
    }
}
