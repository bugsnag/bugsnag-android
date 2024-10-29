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

        val tombstoneEventEnhancer = TombstoneEventEnhancer(
            client.logger,
            configuration.listOpenFds,
            configuration.includeLogcat
        )
        val traceEventEnhancer = TraceEventEnhancer(
            client.logger,
            client.immutableConfig.projectPackages
        )

        val exitInfoPluginStore =
            ExitInfoPluginStore(client.immutableConfig)
        addAllExitInfoAtFirstRun(client, exitInfoPluginStore)
        val (oldPid, exitInfoKeys) = exitInfoPluginStore.load()
        exitInfoPluginStore.persist(android.os.Process.myPid(), exitInfoKeys)

        val exitInfoCallback = createExitInfoCallback(
            client,
            oldPid,
            exitInfoPluginStore,
            tombstoneEventEnhancer,
            traceEventEnhancer
        )
        InternalHooks.setEventStoreEmptyCallback(client) {
            synthesizeNewEvents(
                client,
                exitInfoPluginStore,
                tombstoneEventEnhancer,
                traceEventEnhancer
            )
        }
        client.addOnSend(exitInfoCallback)
    }

    private fun addAllExitInfoAtFirstRun(
        client: Client,
        exitInfoPluginStore: ExitInfoPluginStore
    ) {
        if (exitInfoPluginStore.isFirstRun || exitInfoPluginStore.legacyStore) {
            val am: ActivityManager = client.appContext.safeGetActivityManager() ?: return
            val allExitInfo: List<ApplicationExitInfo> =
                am.getHistoricalProcessExitReasons(
                    client.appContext.packageName,
                    MATCH_ALL,
                    MAX_EXIT_REASONS
                )

            allExitInfo.forEach { exitInfo ->
                exitInfoPluginStore.addExitInfoKey(ExitInfoKey(exitInfo.pid, exitInfo.timestamp))
            }
        }
    }

    private fun createExitInfoCallback(
        client: Client,
        oldPid: Int?,
        exitInfoPluginStore: ExitInfoPluginStore,
        tombstoneEventEnhancer: TombstoneEventEnhancer,
        traceEventEnhancer: TraceEventEnhancer
    ): ExitInfoCallback = ExitInfoCallback(
        client.appContext,
        oldPid,
        tombstoneEventEnhancer,
        traceEventEnhancer,
        exitInfoPluginStore
    )

    private fun synthesizeNewEvents(
        client: Client,
        exitInfoPluginStore: ExitInfoPluginStore,
        tombstoneEventEnhancer: TombstoneEventEnhancer,
        traceEventEnhancer: TraceEventEnhancer
    ) {
        val eventSynthesizer = EventSynthesizer(
            traceEventEnhancer,
            tombstoneEventEnhancer,
            exitInfoPluginStore,
            configuration.reportUnmatchedAnrs,
            configuration.reportUnmatchedNativeCrashes
        )
        val context = client.appContext
        val am: ActivityManager = context.safeGetActivityManager() ?: return
        val allExitInfo: List<ApplicationExitInfo> =
            am.getHistoricalProcessExitReasons(context.packageName, 0, 100)
        allExitInfo.forEach {
            val newEvent = eventSynthesizer.createEventWithExitInfo(it)
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

    companion object {
        private const val MATCH_ALL = 0
        private const val MAX_EXIT_REASONS = 100
    }
}
