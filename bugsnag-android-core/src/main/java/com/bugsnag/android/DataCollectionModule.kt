package com.bugsnag.android

import android.os.Environment
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
    memoryTrimState: MemoryTrimState
) : DependencyModule() {

    private val ctx = contextModule.ctx
    private val cfg = configModule.config
    private val logger = cfg.logger
    private val deviceBuildInfo: DeviceBuildInfo = DeviceBuildInfo.defaultInfo()
    private val dataDir = Environment.getDataDirectory()

    val appDataCollector = AppDataCollector(
        ctx,
        ctx.packageManager,
        cfg,
        trackerModule.sessionTracker,
        systemServiceModule.activityManager,
        trackerModule.launchCrashTracker,
        memoryTrimState
    )

    private lateinit var rootDetector: RootDetector
    private lateinit var _deviceDataCollector: DeviceDataCollector

    val deviceDataCollector: DeviceDataCollector
        get() = resolvedValueOf { _deviceDataCollector }

    override fun resolveDependencies() {
        rootDetector = RootDetector(logger = logger, deviceBuildInfo = deviceBuildInfo)
        _deviceDataCollector = DeviceDataCollector(
            connectivity,
            ctx,
            ctx.resources,
            deviceId,
            deviceBuildInfo,
            dataDir,
            rootDetector,
            bgTaskService,
            logger
        )
    }
}
