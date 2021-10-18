package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig
import java.io.File
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Tracks whether the app is currently in its launch period. This creates a timer of
 * configuration.launchDurationMillis, after which which the launch period is considered
 * complete. If this value is zero, then the user must manually call markLaunchCompleted().
 *
 * A marker file is also created, which is used to detect whether fatal NDK errors were in the
 * launch state.
 */
internal class LaunchCrashTracker @JvmOverloads constructor(
    config: ImmutableConfig,
    private val executor: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(1)
) : BaseObservable() {

    @JvmField
    @Volatile
    internal var launching = false
    private val logger = config.logger
    private val launchPeriodMarkerFile =
        File(config.persistenceDirectory.value, "launch-crash-marker")

    /**
     * Starts automatically tracking the launch period.
     */
    fun startAutoTracking(config: ImmutableConfig) {
        val delay = config.launchDurationMillis
        markLaunchStarted()

        if (delay > 0) {
            executor.executeExistingDelayedTasksAfterShutdownPolicy = false
            try {
                executor.schedule({ markLaunchCompleted() }, delay, TimeUnit.MILLISECONDS)
            } catch (exc: RejectedExecutionException) {
                markLaunchCompleted()
                logger.w("Failed to schedule timer for LaunchCrashTracker", exc)
            }
        }
    }

    /**
     * Determines whether the application crashed duration the launch period in the last
     * application process. It is an error to call this after calling [markLaunchStarted].
     */
    fun crashedDuringLastLaunch(): Boolean {
        return runCatching {
            !isLaunching() && launchPeriodMarkerFile.exists()
        }.getOrDefault(false)
    }

    /**
     * Marks the current launch as started. It is an error to call this more than once,
     * and this must be called before [markLaunchCompleted].
     */
    fun markLaunchStarted() {
        launching = true
        runCatching {
            launchPeriodMarkerFile.createNewFile()
        }.getOrElse { exc ->
            logger.w("Failed to create launch marker file", exc)
        }
    }

    /**
     * Marks the current launch as completed. It is an error to call this more than once,
     * and this must be called after [markLaunchStarted].
     */
    fun markLaunchCompleted() {
        launching = false
        runCatching { executor.shutdown() }
        runCatching {
            launchPeriodMarkerFile.delete()
        }.getOrElse { exc ->
            logger.w("Failed to delete launch marker file", exc)
        }
        updateState { StateEvent.UpdateIsLaunching(false) }
        logger.d("App launch period marked as complete")
    }

    /**
     * Determines whether the application is in the launch period.
     */
    fun isLaunching() = launching
}
