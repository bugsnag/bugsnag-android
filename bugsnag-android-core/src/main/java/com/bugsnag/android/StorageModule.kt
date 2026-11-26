package com.bugsnag.android

import android.content.Context
import android.os.Build
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.BugsnagStoreMigrator.migrateLegacyFiles
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.TaskType
import com.bugsnag.android.internal.dag.BackgroundDependencyModule
import com.bugsnag.android.internal.dag.Provider
import com.bugsnag.android.internal.remoteconfig.RemoteConfigState
import com.bugsnag.android.internal.remoteconfig.RemoteConfigStore
import java.io.File

/**
 * A dependency module which constructs the objects that store information to disk in Bugsnag.
 */
internal class StorageModule(
    appContext: Context,
    private val immutableConfig: ImmutableConfig,
    private val notifier: Notifier,
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

    val deviceId = deviceIdStore.map {
        it.load()
    }

    val userStore = provider {
        UserStore(
            immutableConfig.persistUser,
            bugsnagDir,
            deviceId,
            sharedPrefMigrator = sharedPrefMigrator,
            services = UserStoreServices(
                logger = immutableConfig.logger,
                bgTaskService = bgTaskService
            )
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

    val remoteConfigState = provider {
        val configDir = File(bugsnagDir.get(), "config")
        RemoteConfigState(
            RemoteConfigStore(
                configDir,
                immutableConfig.versionCode ?: 0,
                immutableConfig.logger,
                cooldownDir = remoteConfigCooldownDir(appContext, configDir)
            ),
            immutableConfig,
            notifier,
            bgTaskService,
            deviceIdProvider = { deviceId.getOrNull()?.deviceId }
        )
    }

    val lastRunInfo = lastRunInfoStore.map { lastRunInfoStore ->
        val info = lastRunInfoStore.load()
        val currentRunInfo = LastRunInfo(0, crashed = false, crashedDuringLaunch = false)
        lastRunInfoStore.persist(currentRunInfo)
        return@map info
    }

    /**
     * The cooldown marker is kept out of the cache directory, which Android may clear when storage
     * is low: losing it resets the backoff that protects the config service. It goes in
     * noBackupFilesDir so a restore onto another device cannot bring back a stale cooldown. An app
     * that chose its own persistenceDirectory keeps the marker there, next to the config.
     */
    private fun remoteConfigCooldownDir(appContext: Context, configDir: File): File {
        val persistenceDir = immutableConfig.persistenceDirectory.value
        if (persistenceDir.absoluteFile != appContext.cacheDir?.absoluteFile) {
            return configDir
        }
        val baseDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appContext.noBackupFilesDir
        } else {
            appContext.filesDir
        }
        return baseDir?.let { File(it, "bugsnag/remote-config") } ?: configDir
    }

    fun loadUser(initialUser: User): Provider<UserState> = provider {
        val userState = userStore.get().load(initialUser)
        sharedPrefMigrator.getOrNull()?.deleteLegacyPrefs()
        return@provider userState
    }
}
