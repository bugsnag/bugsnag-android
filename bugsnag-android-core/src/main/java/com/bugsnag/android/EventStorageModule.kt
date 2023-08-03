package com.bugsnag.android

import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.dag.ConfigModule
import com.bugsnag.android.internal.dag.ContextModule
import com.bugsnag.android.internal.dag.DependencyModule
import com.bugsnag.android.internal.dag.SystemServiceModule

/**
 * A dependency module which constructs the objects that persist events to disk in Bugsnag.
 */
internal class EventStorageModule(
    contextModule: ContextModule,
    configModule: ConfigModule,
    dataCollectionModule: DataCollectionModule,
    private val bgTaskService: BackgroundTaskService,
    trackerModule: TrackerModule,
    systemServiceModule: SystemServiceModule,
    private val notifier: Notifier,
    private val callbackState: CallbackState
) : DependencyModule() {

    private val contextModule: ContextModule by dependencyRef(contextModule)
    private val dataCollectionModule: DataCollectionModule by dependencyRef(dataCollectionModule)
    private val trackerModule: TrackerModule by dependencyRef(trackerModule)
    private val systemServiceModule: SystemServiceModule by dependencyRef(systemServiceModule)

    private val cfg = configModule.config

    lateinit var eventStore: EventStore
        private set

    override fun load() {
        val delegate = if (cfg.telemetry.contains(Telemetry.INTERNAL_ERRORS))
            InternalReportDelegate(
                contextModule.ctx,
                cfg.logger,
                cfg,
                systemServiceModule.storageManager,
                dataCollectionModule.appDataCollector,
                dataCollectionModule.deviceDataCollector,
                trackerModule.sessionTracker,
                notifier,
                bgTaskService
            ) else null

        eventStore = EventStore(cfg, cfg.logger, notifier, bgTaskService, delegate, callbackState)
    }
}
