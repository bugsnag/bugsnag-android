package com.bugsnag.android

import android.content.Context
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.BugsnagStoreMigrator.migrateLegacyFiles
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.TaskType
import com.bugsnag.android.internal.dag.BackgroundDependencyModule

/**
 * A dependency module which constructs the objects that store information to disk in Bugsnag.
 */
internal class StorageModule(
    appContext: Context,
    private val immutableConfig: ImmutableConfig,
    bgTaskService: BackgroundTaskService
) : BackgroundDependencyModule(bgTaskService, TaskType.IO) {

    val bugsnagDir = provider {
        migrateLegacyFiles(immutableConfig.persistenceDirectory)
    }

    val sharedPrefMigrator = provider {
        SharedPrefMigrator(appContext)
    }

    val deviceIdStore = provider {
        DeviceIdStore(
            appContext,
            sharedPrefMigrator = sharedPrefMigrator,
            logger = immutableConfig.logger,
            config = immutableConfig
        )
    }

    val userStore = provider {
        UserStore(
            immutableConfig.persistUser,
            bugsnagDir,
            deviceIdStore.map { it.deviceIds },
            sharedPrefMigrator = sharedPrefMigrator,
            logger = immutableConfig.logger
        )
    }

    val lastRunInfoStore = provider {
        LastRunInfoStore(immutableConfig)
    }

    val sessionStore = provider {
        SessionStore(
            bugsnagDir.get(),
            immutableConfig.maxPersistedSessions,
            immutableConfig.apiKey,
            immutableConfig.logger,
            null
        )
    }

    val lastRunInfo = lastRunInfoStore.map { lastRunInfoStore ->
        val info = lastRunInfoStore.load()
        val currentRunInfo = LastRunInfo(0, crashed = false, crashedDuringLaunch = false)
        lastRunInfoStore.persist(currentRunInfo)
        return@map info
    }
}
