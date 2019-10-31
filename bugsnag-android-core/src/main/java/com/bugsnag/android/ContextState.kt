package com.bugsnag.android

internal data class ContextState(var context: String? = null) {
    fun copy() = this.copy(context = context)
}
