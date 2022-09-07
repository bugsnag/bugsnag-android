package com.bugsnag.android.internal

import com.bugsnag.android.NdkPluginCaller

class InternalMetricsImpl : InternalMetrics {
    private val configDifferences = hashMapOf<String, Any>()
    private val callbackCounts = hashMapOf<String, Int>()

    override fun toJsonableMap(): Map<String, Any> {
        return mapOf(
            "config" to configDifferences,
            "callbacks" to allCallbacks()
        )
    }

    override fun setConfigDifferences(differences: Map<String, Any>) {
        configDifferences.clear()
        configDifferences.putAll(differences)
        NdkPluginCaller.setStaticData(mapOf("config" to configDifferences))
    }

    override fun setCallbackCounts(newCallbackCounts: Map<String, Int>) {
        callbackCounts.clear()
        callbackCounts.putAll(newCallbackCounts)
        NdkPluginCaller.initCallbackCounts(newCallbackCounts)
    }

    override fun notifyAddCallback(callback: String) {
        modifyCallback(callback, 1)
        NdkPluginCaller.notifyAddCallback(callback)
    }

    override fun notifyRemoveCallback(callback: String) {
        modifyCallback(callback, -1)
        NdkPluginCaller.notifyRemoveCallback(callback)
    }

    private fun modifyCallback(callback: String, delta: Int) {
        var currentValue = callbackCounts[callback] ?: 0
        currentValue += delta
        callbackCounts[callback] = currentValue.coerceAtLeast(0)
    }

    private fun allCallbacks(): Map<String, Any> {
        val result = hashMapOf<String, Any>()
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
