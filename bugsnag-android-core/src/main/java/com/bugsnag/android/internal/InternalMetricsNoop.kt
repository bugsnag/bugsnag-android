package com.bugsnag.android.internal

import android.util.Log

class InternalMetricsNoop : InternalMetrics {
    override fun toJsonableMap(): Map<String, Any> = emptyMap()
    override fun setConfigDifferences(differences: Map<String, Any>) = Unit
    override fun setCallbackCounts(newCallbackCounts: Map<String, Int>) = Unit
    override fun notifyAddCallback(callback: String) = Unit
    override fun notifyRemoveCallback(callback: String) = Unit
    override fun setMetadataTrimMetrics(stringsTrimmed: Int, charsRemoved: Int) {
        Log.e("###", "### InternalMetricsNoop.setMetadataTrimMetrics ${stringsTrimmed}, ${charsRemoved}")
    }
    override fun setBreadcrumbTrimMetrics(breadcrumbsRemoved: Int, bytesRemoved: Int) = Unit
}
