package com.bugsnag.android

import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.dag.ConfigModule
import com.bugsnag.android.internal.dag.DependencyModule

/**
 * A dependency module which constructs objects that track launch/session related information
 * in Bugsnag.
 */
internal class TrackerModule(
    configModule: ConfigModule,
    storageModule: StorageModule,
    private val client: Client,
    bgTaskService: BackgroundTaskService,
    private val callbackState: CallbackState
) : DependencyModule(bgTaskService) {

    private val config = configModule.config

    private val storageModule: StorageModule by dependencyRef(storageModule)

    val launchCrashTracker = LaunchCrashTracker(config)

    lateinit var sessionTracker: SessionTracker
        private set

    override fun load() {
        sessionTracker = SessionTracker(
            config,
            callbackState,
            client,
            storageModule.sessionStore,
            config.logger,
            bgTaskService
        )
    }
}
