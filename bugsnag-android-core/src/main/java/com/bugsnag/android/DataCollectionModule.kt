package com.bugsnag.android

import android.os.Environment
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.TaskType
import com.bugsnag.android.internal.dag.ConfigModule
import com.bugsnag.android.internal.dag.ContextModule
import com.bugsnag.android.internal.dag.DependencyModule
import com.bugsnag.android.internal.dag.SystemServiceModule
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException

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
    deviceIdStore: Future<DeviceIdStore.DeviceIds?>,
    memoryTrimState: MemoryTrimState
) : DependencyModule() {

    private val ctx = contextModule.ctx
    private val cfg = configModule.config
    private val logger = cfg.logger
    private val deviceBuildInfo: DeviceBuildInfo = DeviceBuildInfo.defaultInfo()
    private val dataDir = Environment.getDataDirectory()

    val appDataCollector =
        AppDataCollector(
            ctx,
            ctx.packageManager,
            cfg,
            trackerModule.sessionTracker,
            systemServiceModule.activityManager,
            trackerModule.launchCrashTracker,
            memoryTrimState
        )

    val deviceDataCollector =
        DeviceDataCollector(
            connectivity,
            ctx,
            ctx.resources,
            deviceIdStore,
            deviceBuildInfo,
            dataDir,
            rootDetectionFuture(bgTaskService),
            bgTaskService,
            logger
        )

    private fun rootDetectionFuture(bgTaskService: BackgroundTaskService): Future<Boolean>? = try {
        bgTaskService.submitTask(
            TaskType.IO,
            Callable {
                val rootDetector = RootDetector(logger = logger, deviceBuildInfo = deviceBuildInfo)
                rootDetector.isRooted()
            }
        )
    } catch (exc: RejectedExecutionException) {
        logger.w("Failed to perform root detection checks", exc)
        null
    }
}
