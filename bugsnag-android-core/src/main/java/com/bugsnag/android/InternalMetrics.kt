package com.bugsnag.android

import java.util.Collections

/**
 * Stores internal metrics for Bugsnag use.
 */
internal class InternalMetrics {
    private var enabled = false
    private val configDifferences = mutableMapOf<String, Any>()
    private val callbackCounts = mutableMapOf<String, Int>()

    fun applyTelemetryConfig(telemetry: Set<Telemetry>) {
        enabled = telemetry.contains(Telemetry.USAGE)
        NdkPluginCaller.setInternalMetricsEnabled(enabled)
    }

    fun toJsonableMap(): Map<String, Any> {
        return if (enabled) {
            mapOf(
                "config" to configDifferences,
                "callbacks" to allCallbacks()
            )
        } else {
            mapOf()
        }
    }

    fun setConfigDifferences(differences: Map<String, Any>) {
        if (enabled) {
            configDifferences.clear()
            configDifferences.putAll(differences)
            NdkPluginCaller.setStaticData(Collections.singletonMap("config", configDifferences))
        }
    }

    fun setCallbackCounts(newCallbackCounts: Map<String, Int>) {
        if (enabled) {
            callbackCounts.clear()
            callbackCounts.putAll(newCallbackCounts)
            NdkPluginCaller.initCallbackCounts(newCallbackCounts)
        }
    }

    fun notifyAddCallback(callback: String) {
        if (enabled) {
            modifyCallback(callback, 1)
            NdkPluginCaller.notifyAddCallback(callback)
        }
    }

    fun notifyRemoveCallback(callback: String) {
        if (enabled) {
            modifyCallback(callback, -1)
            NdkPluginCaller.notifyRemoveCallback(callback)
        }
    }

    private fun modifyCallback(callback: String, delta: Int) {
        var currentValue = callbackCounts[callback]
        if (currentValue == null) {
            currentValue = delta
        } else {
            currentValue += delta
        }
        if (currentValue < 0) {
            currentValue = 0
        }
        callbackCounts[callback] = currentValue
    }

    private fun allCallbacks(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        result.putAll(callbackCounts)

        val counts = NdkPluginCaller.getCurrentCallbackSetCounts()
        if (counts != null) {
            // ndkOnError comes from the native side. The rest we already have.
            val ndkOnError = counts["ndkOnError"]
            if (ndkOnError != null) {
                result["ndkOnError"] = ndkOnError
            }
        }

        val usage = NdkPluginCaller.getCurrentNativeApiCallUsage()
        if (usage != null) {
            result.putAll(usage)
        }

        return result
    }
}
