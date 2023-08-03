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
    private val bgTaskService: BackgroundTaskService,
    private val connectivity: Connectivity,
    private val deviceId: String?,
    private val internalDeviceId: String?,
    private val memoryTrimState: MemoryTrimState
) : DependencyModule() {

    private val ctx = contextModule.ctx
    private val cfg = configModule.config
    private val logger = cfg.logger
    private val deviceBuildInfo: DeviceBuildInfo = DeviceBuildInfo.defaultInfo()
    private val dataDir = Environment.getDataDirectory()

    private val contextModule: ContextModule by dependencyRef(contextModule)
    private val configModule: ConfigModule by dependencyRef(configModule)
    private val systemServiceModule: SystemServiceModule by dependencyRef(systemServiceModule)
    private val trackerModule: TrackerModule by dependencyRef(trackerModule)

    lateinit var appDataCollector: AppDataCollector
        private set

    lateinit var rootDetector: RootDetector
        private set

    lateinit var deviceDataCollector: DeviceDataCollector
        private set

    override fun load() {
        appDataCollector = AppDataCollector(
            ctx,
            ctx.packageManager,
            cfg,
            trackerModule.sessionTracker,
            systemServiceModule.activityManager,
            trackerModule.launchCrashTracker,
            memoryTrimState
        )

        rootDetector = RootDetector(logger = logger, deviceBuildInfo = deviceBuildInfo)

        deviceDataCollector = DeviceDataCollector(
            connectivity,
            ctx,
            ctx.resources,
            deviceId,
            internalDeviceId,
            deviceBuildInfo,
            dataDir,
            rootDetector,
            bgTaskService,
            logger
        )
    }
}
