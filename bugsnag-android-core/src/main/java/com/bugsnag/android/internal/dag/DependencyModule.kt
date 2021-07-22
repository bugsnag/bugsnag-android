package com.bugsnag.android.internal.dag

import com.bugsnag.android.BackgroundTaskService
import com.bugsnag.android.TaskType

/**
 * A collection of related objects which are used to inject dependencies. This is somewhat
 * analogous to Dagger's concept of modules - although this implementation is much simpler.
 */
internal interface DependencyModule {

    /**
     * Blocks until all dependencies in the module have been constructed. This provides the option
     * for modules to construct objects in a background thread, then have a user block on another
     * thread until all the objects have been constructed.
     */
    fun resolveDependencies(bgTaskService: BackgroundTaskService, taskType: TaskType) = Unit
}

/**
 * Creates a [Future] for loading objects on the IO thread.
 */
internal fun loadDepModuleIoObjects(bgTaskService: BackgroundTaskService, action: () -> Unit) =
    runCatching {
        bgTaskService.submitTask(
            TaskType.IO,
            Runnable(action)
        )
    }.getOrNull()
