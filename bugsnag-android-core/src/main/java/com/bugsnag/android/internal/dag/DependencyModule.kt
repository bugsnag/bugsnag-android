package com.bugsnag.android.internal.dag

import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.TaskType
import java.util.concurrent.Future
import kotlin.reflect.KProperty

private typealias DependencyRef = Int

internal abstract class DependencyModule : Runnable {

    private val dependencies = mutableListOf<DependencyModule>()

    private var state: Int = STATE_PENDING

    fun enqueue(bgTaskService: BackgroundTaskService): Future<*> {
        return bgTaskService.submitTask(TaskType.DEFAULT, this)
    }

    open fun load() = Unit

    override fun run() {
        ensureLoaded()
    }

    @Synchronized
    private fun ensureLoaded() {
        when (state) {
            STATE_LOADING -> {
                while (state == STATE_LOADING) {
                    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                    (this as Object).wait()
                }
            }

            STATE_PENDING -> {
                state = STATE_LOADING
                try {
                    dependencies.forEach(DependencyModule::ensureLoaded)
                    load()
                } finally {
                    state = STATE_LOADED

                    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                    (this as Object).notifyAll()
                }
            }
        }
    }

    @Synchronized
    fun await() {
        while (state != STATE_LOADED) {
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            (this as Object).wait()
        }
    }

    protected fun dependencyRef(module: DependencyModule): DependencyRef {
        val id = dependencies.size
        dependencies.add(module)
        return id
    }

    protected operator fun <T> DependencyRef.getValue(thisRef: Any?, property: KProperty<*>): T {
        @Suppress("UNCHECKED_CAST")
        return dependencies[this] as T
    }

    private companion object {
        const val STATE_PENDING = 0
        const val STATE_LOADING = 1
        const val STATE_LOADED = 2
    }
}
