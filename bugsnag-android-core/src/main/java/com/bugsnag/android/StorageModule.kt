package com.bugsnag.android

import android.content.Context
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.BugsnagStoreMigrator.migrateLegacyFiles
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.TaskType
import com.bugsnag.android.internal.dag.BackgroundDependencyModule
import com.bugsnag.android.internal.dag.Provider

/**
 * A dependency module which constructs the objects that store information to disk in Bugsnag.
 */
internal class StorageModule(
    appContext: Context,
    private val immutableConfig: ImmutableConfig,
    bgTaskService: BackgroundTaskService,
    performanceInstrumentation: PerformanceInstrumentation<Any>
) : BackgroundDependencyModule(bgTaskService, performanceInstrumentation, TaskType.IO) {

    val bugsnagDir = provider("migrateLegacyFiles") {
        migrateLegacyFiles(immutableConfig.persistenceDirectory)
    }

    val sharedPrefMigrator = provider("SharedPrefMigrator") {
        SharedPrefMigrator(appContext)
    }

    val deviceIdStore = provider("DeviceIdStore") {
        DeviceIdStore(
            appContext,
            sharedPrefMigrator = sharedPrefMigrator,
            logger = immutableConfig.logger,
            config = immutableConfig
        )
    }

    val deviceId = deviceIdStore.map {
        it.load()
    }

    val userStore = provider("UserStore") {
        UserStore(
            immutableConfig.persistUser,
            bugsnagDir,
            deviceId,
            sharedPrefMigrator = sharedPrefMigrator,
            logger = immutableConfig.logger
        )
    }

    val lastRunInfoStore = provider("LastRunInfoStore") {
        LastRunInfoStore(immutableConfig)
    }

    val sessionStore = provider("SessionStore") {
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

    fun loadUser(initialUser: User): Provider<UserState> = provider("loadUser") {
        val userState = userStore.get().load(initialUser)
        sharedPrefMigrator.getOrNull()?.deleteLegacyPrefs()
        return@provider userState
    }
}
