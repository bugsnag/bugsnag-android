package com.bugsnag.android.internal

class InternalMetricsNoop : InternalMetrics {
    override fun toJsonableMap(): Map<String, Any> = emptyMap()
    override fun setConfigDifferences(differences: Map<String, Any>) = Unit
    override fun setCallbackCounts(newCallbackCounts: Map<String, Int>) = Unit
    override fun notifyAddCallback(callback: String) = Unit
    override fun notifyRemoveCallback(callback: String) = Unit
}
