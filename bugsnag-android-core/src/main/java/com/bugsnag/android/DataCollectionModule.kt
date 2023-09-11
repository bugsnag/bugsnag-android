package com.bugsnag.android

import android.os.Environment
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.dag.ConfigModule
import com.bugsnag.android.internal.dag.ContextModule
import com.bugsnag.android.internal.dag.DependencyModule
import com.bugsnag.android.internal.dag.SystemServiceModule

/**
 * A dependency module which constructs the objects that collect data in Bugsnag. For example, this
 * class is responsible for creating classes which capture device-specific information.
 */
internal class DataCollectionModule(
    contextModule: ContextModule,
    configModule: ConfigModule,
    systemServiceModule: SystemServiceModule,
    trackerModule: TrackerModule,
    storageModule: StorageModule,
    bgTaskService: BackgroundTaskService,
    private val connectivity: Connectivity,
    private val memoryTrimState: MemoryTrimState
) : DependencyModule(bgTaskService) {

    private val deviceBuildInfo: DeviceBuildInfo = DeviceBuildInfo.defaultInfo()
    private val dataDir = Environment.getDataDirectory()

    private val contextModule: ContextModule by dependencyRef(contextModule)
    private val configModule: ConfigModule by dependencyRef(configModule)
    private val systemServiceModule: SystemServiceModule by dependencyRef(systemServiceModule)
    private val trackerModule: TrackerModule by dependencyRef(trackerModule)
    private val storageModule: StorageModule by dependencyRef(storageModule)

    lateinit var appDataCollector: AppDataCollector
        private set

    lateinit var rootDetector: RootDetector
        private set

    lateinit var deviceDataCollector: DeviceDataCollector
        private set

    override fun load() {
        val ctx = contextModule.ctx
        val cfg = configModule.config

        appDataCollector = AppDataCollector(
            ctx,
            ctx.packageManager,
            cfg,
            trackerModule.sessionTracker,
            systemServiceModule.activityManager,
            trackerModule.launchCrashTracker,
            memoryTrimState
        )

        rootDetector = RootDetector(logger = cfg.logger, deviceBuildInfo = deviceBuildInfo)

        deviceDataCollector = DeviceDataCollector(
            connectivity,
            ctx,
            ctx.resources,
            storageModule.deviceId,
            storageModule.internalDeviceId,
            deviceBuildInfo,
            dataDir,
            rootDetector,
            bgTaskService,
            cfg.logger
        )
    }
}
