package com.bugsnag.android

import com.bugsnag.android.internal.dag.Provider

class ValueProvider<E>(private val value: E) : Provider<E> {
    override fun getOrNull(): E? = value
    override fun get(): E = value
}
