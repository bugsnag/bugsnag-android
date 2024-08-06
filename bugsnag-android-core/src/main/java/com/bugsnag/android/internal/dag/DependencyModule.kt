package com.bugsnag.android.internal.dag

import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.TaskType

internal interface DependencyModule

internal abstract class BackgroundDependencyModule(
    @JvmField
    val bgTaskService: BackgroundTaskService,
    @JvmField
    val taskType: TaskType = TaskType.DEFAULT
) : DependencyModule {
    inline fun <R> provider(crossinline provider: () -> R): RunnableProvider<R> {
        return bgTaskService.provider(taskType, provider)
    }

    internal inline fun <E, R> Provider<E>.map(crossinline mapping: (E) -> R): RunnableProvider<R> {
        val task = object : RunnableProvider<R>() {
            override fun invoke(): R = mapping(this@map.get())
        }

        bgTaskService.execute(taskType, task)
        return task
    }
}
