package com.bugsnag.android

import com.bugsnag.android.internal.dag.ConfigModule
import com.bugsnag.android.internal.dag.ContextModule
import com.bugsnag.android.internal.dag.DependencyModule
import com.bugsnag.android.internal.dag.SystemServiceModule
import com.bugsnag.android.internal.dag.loadDepModuleIoObjects
import java.util.concurrent.Future

/**
 * A dependency module which constructs the objects that persist events to disk in Bugsnag.
 */
internal class EventStorageModule(
    contextModule: ContextModule,
    configModule: ConfigModule,
    dataCollectionModule: DataCollectionModule,
    bgTaskService: BackgroundTaskService,
    trackerModule: TrackerModule,
    systemServiceModule: SystemServiceModule,
    notifier: Notifier
) : DependencyModule {

    private val cfg = configModule.config

    private val delegate by lazy {
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
        )
    }

    val eventStore by lazy { EventStore(cfg, cfg.logger, notifier, bgTaskService, delegate) }

    // trigger initialization on a background thread. Client<init> will then block on the main
    // thread if these have not completed by the appropriate time.
    private val future: Future<*>? = loadDepModuleIoObjects(bgTaskService) { eventStore }

    override fun resolveDependencies(bgTaskService: BackgroundTaskService, taskType: TaskType) {
        runCatching {
            future?.get()
        }
    }
}
