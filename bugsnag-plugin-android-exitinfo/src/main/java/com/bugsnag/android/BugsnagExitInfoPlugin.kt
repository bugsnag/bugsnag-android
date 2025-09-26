package com.bugsnag.android

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.bugsnag.android.ApplicationExitInfoMatcher.Companion.MATCH_ALL
import com.bugsnag.android.ApplicationExitInfoMatcher.Companion.MAX_EXIT_INFO
import com.bugsnag.android.internal.TaskType

@RequiresApi(Build.VERSION_CODES.R)
class BugsnagExitInfoPlugin @JvmOverloads constructor(
    configuration: ExitInfoPluginConfiguration = ExitInfoPluginConfiguration()
) : Plugin {

    private lateinit var internalHooks: InternalHooks

    private val configuration = configuration.copy()
    private var applicationExitInfo: List<ApplicationExitInfo> = emptyList()

    override fun load(client: Client) {
        internalHooks = InternalHooks(client)

        if (!configuration.disableProcessStateSummaryOverride) {
            client.addOnSession(SessionProcessStateSummaryCallback(client))
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

        val exitInfoPluginStore = ExitInfoPluginStore(client.immutableConfig)
        addAllExitInfoAtFirstRun(client, exitInfoPluginStore)
        applicationExitInfo = getHistoricExitReasons(exitInfoPluginStore, client)

        val exitInfoMatcher = ApplicationExitInfoMatcher(
            applicationExitInfo,
            exitInfoPluginStore.previousState
        )

        val exitInfoCallback = createExitInfoCallback(
            exitInfoPluginStore,
            tombstoneEventEnhancer,
            traceEventEnhancer,
            exitInfoMatcher
        )
        client.addOnSend(exitInfoCallback)

        if (client.appContext.isPrimaryProcess() &&
            !configuration.disableProcessStateSummaryOverride
        ) {
            configureEventSynthesizer(
                exitInfoPluginStore,
                traceEventEnhancer,
                exitInfoMatcher
            )
        }
    }

    private fun configureEventSynthesizer(
        exitInfoPluginStore: ExitInfoPluginStore,
        traceEventEnhancer: TraceEventEnhancer,
        exitInfoMatcher: ApplicationExitInfoMatcher
    ) {
        internalHooks.setDiscardEventCallback { eventPayload ->
            // we track all of the discarded events as "processed" so that we do not
            // synthesize them again later
            val event = eventPayload.event
            if (event != null) {
                val exitInfo = exitInfoMatcher.matchExitInfo(event)
                exitInfo?.let { exitInfoPluginStore.addExitInfoKey(ExitInfoKey(exitInfo)) }
            }
        }

        internalHooks.setEventStoreEmptyCallback {
            synthesizeNewEventsIfRequired(exitInfoPluginStore, traceEventEnhancer)
        }
    }

    private fun addAllExitInfoAtFirstRun(
        client: Client,
        exitInfoPluginStore: ExitInfoPluginStore
    ) {
        if (exitInfoPluginStore.previousState == null) {
            val am: ActivityManager = client.appContext.safeGetActivityManager() ?: return
            val allExitInfo: List<ApplicationExitInfo> =
                am.getHistoricalProcessExitReasons(
                    client.appContext.packageName,
                    MATCH_ALL,
                    MAX_EXIT_INFO
                )

            exitInfoPluginStore.addExitInfoKeys(allExitInfo.map { ExitInfoKey(it) })
        }
    }

    private fun createExitInfoCallback(
        exitInfoPluginStore: ExitInfoPluginStore,
        tombstoneEventEnhancer: TombstoneEventEnhancer,
        traceEventEnhancer: TraceEventEnhancer,
        applicationExitInfoMatcher: ApplicationExitInfoMatcher
    ): ExitInfoCallback = ExitInfoCallback(
        applicationExitInfo,
        tombstoneEventEnhancer,
        traceEventEnhancer,
        exitInfoPluginStore,
        applicationExitInfoMatcher
    )

    private fun synthesizeNewEventsIfRequired(
        exitInfoPluginStore: ExitInfoPluginStore,
        traceEventEnhancer: TraceEventEnhancer
    ) {
        val eventSynthesizer = EventSynthesizer(
            internalHooks::createEmptyANR,
            traceEventEnhancer,
            exitInfoPluginStore,
            configuration.reportUnmatchedANR
        )

        applicationExitInfo.forEach { exitInfo ->
            val synthesizedEvent = eventSynthesizer.createEventWithExitInfo(exitInfo)
            if (synthesizedEvent != null) {
                exitInfoPluginStore.addExitInfoKey(ExitInfoKey(exitInfo))
                internalHooks.deliver(synthesizedEvent)
            }
        }
    }

    private fun getHistoricExitReasons(
        exitInfoPluginStore: ExitInfoPluginStore,
        client: Client
    ) = exitInfoPluginStore.previousState
        ?.filterApplicationExitInfo(
            client.appContext.safeGetActivityManager()
                ?.getHistoricalProcessExitReasons(
                    client.appContext.packageName,
                    MATCH_ALL,
                    MAX_EXIT_INFO
                )
                .orEmpty()
        )
        .orEmpty()

    override fun unload() = Unit

    private fun Context.isPrimaryProcess(): Boolean {
        return Application.getProcessName() == packageName
    }

    private class SessionProcessStateSummaryCallback(
        private val client: Client
    ) : OnSessionCallback {
        override fun onSession(session: Session): Boolean {
            val am = client.appContext.safeGetActivityManager() ?: return true
            client.bgTaskService.submitTask(TaskType.DEFAULT) {
                try {
                    am.setProcessStateSummary(session.id.toByteArray())
                } catch (e: Exception) {
                    // this can be rate limited by the system, so we ignore it
                }
            }
            return true
        }
    }
}

internal fun Context.safeGetActivityManager(): ActivityManager? = try {
    getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
} catch (e: Exception) {
    null
}
