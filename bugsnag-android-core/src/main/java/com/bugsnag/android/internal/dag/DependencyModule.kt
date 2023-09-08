package com.bugsnag.android.internal.dag

import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.TaskType
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import kotlin.reflect.KProperty

internal abstract class DependencyModule {

    private val properties = mutableListOf<FutureTask<*>>()

    /**
     * Creates a new [Lazy] property that is marked as an object that should be resolved off the
     * main thread when [resolveDependencies] is called.
     */
    fun <T> future(initializer: () -> T): FutureTask<T> {
        val lazy = FutureTask(initializer)
        properties.add(lazy)
        return lazy
    }

    protected operator fun <T> Future<T>.getValue(thisRef: Any, property: KProperty<*>): T {
        return try {
            get()
        } catch (ex: Exception) {
            @Suppress("UNCHECKED_CAST")
            null as T
        }
    }

    /**
     * Blocks until all dependencies in the module have been constructed. This provides the option
     * for modules to construct objects in a background thread, then have a user block on another
     * thread until all the objects have been constructed.
     */
    fun resolveDependencies(bgTaskService: BackgroundTaskService, taskType: TaskType) {
        kotlin.runCatching {
            properties.forEach { property ->
                bgTaskService.execute(property, taskType)
            }
        }
    }
}
