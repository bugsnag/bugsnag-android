package com.bugsnag.android.internal.dag

import androidx.annotation.WorkerThread
import com.bugsnag.android.BackgroundTaskService
import com.bugsnag.android.TaskType
import java.util.concurrent.Callable

internal abstract class DependencyModule {

    @Volatile
    internal var dependenciesResolved = false

    inline fun <R> resolvedValueOf(value: () -> R): R {
        synchronized(this) {
            while (!dependenciesResolved) {
                // The probability that we actually need to wait for the dependencies to be resolved
                // is quite low, so we don't want the overhead (especially during startup) or a
                // ReentrantLock. Instead we want to use the Java wait() and notify() methods
                // so we can leverage monitor locks, which (at time of writing) typically have
                // no allocation cost (until there is contention)
                // https://android.googlesource.com/platform/art/+/master/runtime/monitor.cc#57
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                (this as Object).wait()
            }
        }

        return value()
    }

    @WorkerThread
    protected open fun resolveDependencies() {
    }

    /**
     * Blocks until all dependencies in the module have been constructed. This provides the option
     * for modules to construct objects in a background thread, then have a user block on another
     * thread until all the objects have been constructed.
     */
    open fun resolveDependencies(bgTaskService: BackgroundTaskService, taskType: TaskType) {
        try {
            bgTaskService.submitTask(
                taskType,
                // Callable<Unit> avoids wrapping the Runnable in a Callable
                Callable {
                    synchronized(this) {
                        dependenciesResolved = true
                        resolveDependencies()

                        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                        (this as Object).notifyAll()
                    }
                }
            )
        } catch (exception: Exception) {
            // ignore failures
        }
    }
}
