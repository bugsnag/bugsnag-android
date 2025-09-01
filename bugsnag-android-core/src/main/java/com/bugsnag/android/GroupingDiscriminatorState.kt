package com.bugsnag.android

import java.util.concurrent.atomic.AtomicReference

internal class GroupingDiscriminatorState : BaseObservable() {
    private val groupingDiscriminator = AtomicReference<String?>(null)

    fun setGroupingDiscriminator(value: String?): String? {
        val old = groupingDiscriminator.getAndSet(value)
        emitObservableEvent(value)
        return old
    }

    fun getGroupingDiscriminator(): String? = groupingDiscriminator.get()

    fun emitObservableEvent(value: String?) =
        updateState { StateEvent.UpdateGroupingDiscriminator(value) }
}
