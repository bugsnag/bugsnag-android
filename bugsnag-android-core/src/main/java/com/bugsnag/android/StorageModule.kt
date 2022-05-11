package com.bugsnag.android

import android.content.Context
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.dag.DependencyModule

/**
 * A dependency module which constructs the objects that store information to disk in Bugsnag.
 */
internal class StorageModule(
    private val appContext: Context,
    private val immutableConfig: ImmutableConfig,
    private val logger: Logger
) : DependencyModule() {

    private lateinit var _sharedPrefMigrator: SharedPrefMigrator
    private lateinit var _userStore: UserStore
    private lateinit var _lastRunInfoStore: LastRunInfoStore
    private lateinit var _sessionStore: SessionStore

    private var _deviceId: String? = null
    private var _lastRunInfo: LastRunInfo? = null

    val sharedPrefMigrator get() = resolvedValueOf { _sharedPrefMigrator }
    val deviceId get() = resolvedValueOf { _deviceId }
    val userStore get() = resolvedValueOf { _userStore }
    val lastRunInfoStore get() = resolvedValueOf { _lastRunInfoStore }
    val sessionStore get() = resolvedValueOf { _sessionStore }
    val lastRunInfo get() = resolvedValueOf { _lastRunInfo }

    override fun resolveDependencies() {
        _sharedPrefMigrator = SharedPrefMigrator(appContext)
        _deviceId = resolveDeviceId()

        _userStore = UserStore(
            immutableConfig,
            _deviceId,
            sharedPrefMigrator = _sharedPrefMigrator,
            logger = logger
        )

        _lastRunInfoStore = LastRunInfoStore(immutableConfig)
        _sessionStore = SessionStore(immutableConfig, logger, null)

        _lastRunInfo = resolveLastRunInfo()
    }

    private fun resolveDeviceId(): String? {
        val deviceIdStore = DeviceIdStore(
            appContext,
            sharedPrefMigrator = _sharedPrefMigrator,
            logger = logger
        )

        return deviceIdStore.loadDeviceId()
    }

    private fun resolveLastRunInfo(): LastRunInfo? {
        val info = lastRunInfoStore.load()
        val currentRunInfo = LastRunInfo(0, crashed = false, crashedDuringLaunch = false)
        lastRunInfoStore.persist(currentRunInfo)
        return info
    }
}
