package com.bugsnag.android

import android.content.Context
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.TaskType
import com.bugsnag.android.internal.dag.DependencyModule

/**
 * A dependency module which constructs the objects that store information to disk in Bugsnag.
 */
internal class StorageModule(
    appContext: Context,
    immutableConfig: ImmutableConfig,
    bgTaskService: BackgroundTaskService
) : DependencyModule() {

    val sharedPrefMigrator = bgTaskService.submitTask(
        TaskType.IO,
        SharedPrefMigrator(appContext)
    )

    val deviceIdStore = bgTaskService.submitTask(
        TaskType.IO,
        DeviceIdStore(
            appContext,
            sharedPrefMigrator = sharedPrefMigrator,
            logger = immutableConfig.logger,
            config = immutableConfig
        )
    )

    val userStore = UserStore(
        immutableConfig,
        deviceIdStore,
        sharedPrefMigrator = sharedPrefMigrator,
        logger = immutableConfig.logger
    )

    val lastRunInfoStore = LastRunInfoStore(immutableConfig)

    val sessionStore =
        SessionStore(
            immutableConfig,
            immutableConfig.logger,
            null
        )

    val lastRunInfo = lastRunInfo()

    private fun lastRunInfo(): LastRunInfo? {
        val info = lastRunInfoStore.load()
        val currentRunInfo = LastRunInfo(0, crashed = false, crashedDuringLaunch = false)
        lastRunInfoStore.persist(currentRunInfo)
        return info
    }
}
