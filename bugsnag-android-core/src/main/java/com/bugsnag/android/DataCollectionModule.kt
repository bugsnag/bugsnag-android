package com.bugsnag.android

import android.os.Environment
import com.bugsnag.android.internal.dag.ConfigModule
import com.bugsnag.android.internal.dag.ContextModule
import com.bugsnag.android.internal.dag.DependencyModule
import com.bugsnag.android.internal.dag.SystemServiceModule
import com.bugsnag.android.internal.dag.loadDepModuleIoObjects
import java.util.concurrent.Future

/**
 * A dependency module which constructs the objects that collect data in Bugsnag. For example, this
 * class is responsible for creating classes which capture device-specific information.
 */
internal class DataCollectionModule(
    contextModule: ContextModule,
    configModule: ConfigModule,
    systemServiceModule: SystemServiceModule,
    trackerModule: TrackerModule,
    bgTaskService: BackgroundTaskService,
    connectivity: Connectivity,
    deviceId: String?
) : DependencyModule {

    private val ctx = contextModule.ctx
    private val cfg = configModule.config
    private val logger = cfg.logger
    private val deviceBuildInfo: DeviceBuildInfo = DeviceBuildInfo.defaultInfo()
    private val dataDir = Environment.getDataDirectory()

    val appDataCollector by lazy {
        AppDataCollector(
            ctx,
            ctx.packageManager,
            cfg,
            trackerModule.sessionTracker,
            systemServiceModule.activityManager,
            trackerModule.launchCrashTracker,
            logger
        )
    }

    private val rootDetector by lazy {
        RootDetector(logger = logger, deviceBuildInfo = deviceBuildInfo)
    }

    val deviceDataCollector by lazy {
        DeviceDataCollector(
            connectivity, ctx,
            ctx.resources, deviceId, deviceBuildInfo, dataDir,
            rootDetector, bgTaskService, logger
        )
    }

    // trigger initialization on a background thread. Client<init> will then block on the main
    // thread with resolveDependencies() if these have not completed by the appropriate time.
    private val future: Future<*>? = loadDepModuleIoObjects(bgTaskService) { rootDetector }

    override fun resolveDependencies(bgTaskService: BackgroundTaskService, taskType: TaskType) {
        runCatching {
            future?.get()
        }
    }
}
