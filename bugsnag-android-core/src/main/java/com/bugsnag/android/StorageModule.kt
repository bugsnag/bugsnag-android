package com.bugsnag.android

import android.content.Context
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.dag.DependencyModule
import com.bugsnag.android.internal.dag.loadDepModuleIoObjects
import java.util.concurrent.Future

/**
 * A dependency module which constructs the objects that store information to disk in Bugsnag.
 */
internal class StorageModule(
    appContext: Context,
    immutableConfig: ImmutableConfig,
    bgTaskService: BackgroundTaskService,
    logger: Logger
) : DependencyModule {

    val sharedPrefMigrator by lazy { SharedPrefMigrator(appContext) }

    private val deviceIdStore by lazy {
        DeviceIdStore(
            appContext,
            sharedPrefMigrator = sharedPrefMigrator,
            logger = logger
        )
    }

    val deviceId by lazy { deviceIdStore.loadDeviceId() }

    val userStore by lazy {
        UserStore(
            immutableConfig,
            deviceId,
            sharedPrefMigrator = sharedPrefMigrator,
            logger = logger
        )
    }

    val lastRunInfoStore by lazy { LastRunInfoStore(immutableConfig) }

    val sessionStore by lazy { SessionStore(immutableConfig, logger, null) }

    val lastRunInfo by lazy {
        val info = lastRunInfoStore.load()
        val currentRunInfo = LastRunInfo(0, crashed = false, crashedDuringLaunch = false)
        lastRunInfoStore.persist(currentRunInfo)
        info
    }

    // trigger initialization on a background thread. Client<init> will then block on the main
    // thread if these have not completed by the appropriate time.
    private val future: Future<*>? = loadDepModuleIoObjects(bgTaskService) {
        sharedPrefMigrator
        deviceIdStore
        deviceId
        userStore
        lastRunInfoStore
        lastRunInfo
        sessionStore
    }

    override fun resolveDependencies(bgTaskService: BackgroundTaskService, taskType: TaskType) {
        runCatching {
            future?.get()
        }
    }
}
