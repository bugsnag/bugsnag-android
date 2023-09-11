package com.bugsnag.android.internal.dag

import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.TaskType
import kotlin.reflect.KProperty

private typealias DependencyRef = Int

internal abstract class DependencyModule(
    protected val bgTaskService: BackgroundTaskService
) : Runnable {

    private val dependencies = mutableListOf<DependencyModule>()

    private val dependents = mutableListOf<DependencyModule>()

    private var state: Int = STATE_PENDING

    private var loadedDependencies = 0

    fun enqueue() {
        if (state == STATE_PENDING) {
            state = STATE_ENQUEUED

            // make sure all of our dependencies are enqueued before us
            dependencies.forEach { it.enqueue() }

            // enqueue ourselves
            bgTaskService.submitTask(TaskType.DEFAULT, this)
        }
    }

    open fun load() = Unit

    @Synchronized
    override fun run() {
        if (hasPendingDependencies()) {
            // go back to a PENDING state so that we can be enqueued later
            state = STATE_PENDING
            return
        }

        state = STATE_LOADING
        try {
            load()
        } finally {
            state = STATE_LOADED
            notifyDependents()

            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            (this as Object).notifyAll()
        }
    }

    private fun notifyDependents() {
        dependents.forEach { it.onDependencyLoaded() }
    }

    private fun hasPendingDependencies() = loadedDependencies < dependencies.size

    @Synchronized
    private fun onDependencyLoaded() {
        loadedDependencies++

        if (!hasPendingDependencies()) {
            enqueue()
        }
    }

    protected fun dependencyRef(module: DependencyModule): DependencyRef {
        val id = dependencies.size
        dependencies.add(module)

        if (!module.isLoaded()) {
            module.registerDependant(this)
        } else {
            loadedDependencies++
        }

        return id
    }

    private fun registerDependant(dependant: DependencyModule) {
        dependents.add(dependant)
    }

    @Synchronized
    private fun isLoaded() = state == STATE_LOADED

    fun await() {
        // make sure we're enqueued
        enqueue()

        while (!isLoaded()) {
            synchronized(this) {
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                (this as Object).wait()
            }
        }
    }

    protected operator fun <T> DependencyRef.getValue(thisRef: Any?, property: KProperty<*>): T {
        @Suppress("UNCHECKED_CAST")
        return dependencies[this] as T
    }

    private companion object {
        const val STATE_PENDING = 1
        const val STATE_ENQUEUED = 2
        const val STATE_LOADING = 3
        const val STATE_LOADED = 4
    }
}
