package com.bugsnag.android

import android.content.Context
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.dag.DependencyModule

/**
 * A dependency module which constructs the objects that store information to disk in Bugsnag.
 */
internal class StorageModule(
    private val appContext: Context,
    private val immutableConfig: ImmutableConfig,
    private val logger: Logger,
    bgTaskService: BackgroundTaskService
) : DependencyModule(bgTaskService) {

    lateinit var sharedPrefMigrator: SharedPrefMigrator
        private set

    var deviceId: String? = null
        private set

    var internalDeviceId: String? = null
        private set

    lateinit var userStore: UserStore
        private set

    lateinit var lastRunInfoStore: LastRunInfoStore
        private set

    lateinit var sessionStore: SessionStore
        private set

    var lastRunInfo: LastRunInfo? = null
        private set

    override fun load() {
        sharedPrefMigrator = SharedPrefMigrator(appContext)

        val deviceIdStore = DeviceIdStore(
            appContext,
            sharedPrefMigrator = sharedPrefMigrator,
            logger = logger
        )

        deviceId = deviceIdStore.loadDeviceId()
        internalDeviceId = deviceIdStore.loadInternalDeviceId()

        userStore = UserStore(
            immutableConfig,
            deviceId,
            sharedPrefMigrator = sharedPrefMigrator,
            logger = logger
        )

        lastRunInfoStore = LastRunInfoStore(immutableConfig)
        sessionStore = SessionStore(immutableConfig, logger, null)
        lastRunInfo = loadAndUpdateLastRunInfo()
    }

    private fun loadAndUpdateLastRunInfo(): LastRunInfo? {
        val info = lastRunInfoStore.load()
        val currentRunInfo = LastRunInfo(0, crashed = false, crashedDuringLaunch = false)
        lastRunInfoStore.persist(currentRunInfo)
        return info
    }
}
