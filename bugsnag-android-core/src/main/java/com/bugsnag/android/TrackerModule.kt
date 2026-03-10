package com.bugsnag.android

import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.dag.BackgroundDependencyModule
import com.bugsnag.android.internal.dag.ConfigModule

/**
 * A dependency module which constructs objects that track launch/session related information
 * in Bugsnag.
 */
internal class TrackerModule(
    configModule: ConfigModule,
    storageModule: StorageModule,
    client: Client,
    bgTaskService: BackgroundTaskService,
    callbackState: CallbackState,
    performanceInstrumentation: PerformanceInstrumentation<Any>
) : BackgroundDependencyModule(bgTaskService, performanceInstrumentation) {

    private val config = configModule.config

    val launchCrashTracker = LaunchCrashTracker(config)

    val sessionTracker = provider("SessionTracker") {
        client.config
        SessionTracker(
            config,
            callbackState,
            client,
            storageModule.sessionStore,
            config.logger,
            bgTaskService
        )
    }
}
