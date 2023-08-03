package com.bugsnag.android.internal.dag

import kotlin.reflect.KProperty

private typealias DependencyRef = Int

internal abstract class DependencyModule {

    private val dependencies = mutableListOf<DependencyModule>()

    private var state: Int = STATE_PENDING

    open fun load() = Unit

    fun ensureLoaded() {
        if (state != 0) {
            return
        }

        state = STATE_LOADING
        dependencies.forEach(DependencyModule::ensureLoaded)
        load()
        state = STATE_LOADED
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
