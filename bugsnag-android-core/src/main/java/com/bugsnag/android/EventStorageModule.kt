package com.bugsnag.android

import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.DeliveryPipeline
import com.bugsnag.android.internal.dag.BackgroundDependencyModule
import com.bugsnag.android.internal.dag.ConfigModule
import com.bugsnag.android.internal.dag.ContextModule
import com.bugsnag.android.internal.dag.SystemServiceModule

/**
 * A dependency module which constructs the objects that persist events to disk in Bugsnag.
 */
internal class EventStorageModule(
    contextModule: ContextModule,
    configModule: ConfigModule,
    dataCollectionModule: DataCollectionModule,
    trackerModule: TrackerModule,
    systemServiceModule: SystemServiceModule,
    dependencies: EventStorageDependencies
) : BackgroundDependencyModule(dependencies.bgTaskService) {

    private val cfg = configModule.config

    private val delegate = provider {
        if (cfg.telemetry.contains(Telemetry.INTERNAL_ERRORS))
            InternalReportDelegate(
                contextModule.ctx,
                cfg.logger,
                cfg,
                systemServiceModule.storageManager,
                dataCollectionModule.appDataCollector,
                dataCollectionModule.deviceDataCollector,
                trackerModule.sessionTracker,
                dependencies.notifier,
                dependencies.bgTaskService
            ) else null
    }

    val eventStore = provider {
        EventStore(
            cfg,
            cfg.logger,
            dependencies.notifier,
            dependencies.bgTaskService,
            delegate,
            dependencies.deliveryPipeline
        )
    }
}

internal data class EventStorageDependencies(
    val notifier: Notifier,
    val deliveryPipeline: DeliveryPipeline,
    val bgTaskService: BackgroundTaskService
)
