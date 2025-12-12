package com.bugsnag.android.internal

import com.bugsnag.android.NdkPluginCaller

class InternalMetricsImpl internal constructor(
    source: Map<String, Any>? = null
) : InternalMetrics {
    private val configDifferences: MutableMap<String, Any>
    private val callbackCounts: MutableMap<String, Int>
    private var metadataStringsTrimmedCount = 0
    private var metadataCharsTruncatedCount = 0
    private var breadcrumbsRemovedCount = 0
    private var breadcrumbBytesRemovedCount = 0

    override var remoteConfigEnabled: Boolean = false

    init {
        @Suppress("UNCHECKED_CAST")
        if (source != null) {
            configDifferences = (source["config"] as? MutableMap<String, Any>) ?: hashMapOf()
            callbackCounts = (source["callbacks"] as? MutableMap<String, Int>) ?: hashMapOf()
            remoteConfigEnabled = (source["remoteConfig"] as? Boolean) == true

            val system = source["system"] as? MutableMap<String, Any>
            if (system != null) {
                metadataStringsTrimmedCount = system.getInt("stringsTruncated")
                metadataCharsTruncatedCount = system.getInt("stringCharsTruncated")
                breadcrumbsRemovedCount = system.getInt("breadcrumbsRemovedCount")
                breadcrumbBytesRemovedCount = system.getInt("breadcrumbBytesRemoved")
            }
        } else {
            configDifferences = hashMapOf()
            callbackCounts = hashMapOf()
        }
    }

    override fun toJsonableMap(): Map<String, Any> {
        val callbacks = allCallbacks()

        val system = HashMap<String, Any>().apply {
            putCount("stringsTruncated", metadataStringsTrimmedCount)
            putCount("stringCharsTruncated", metadataCharsTruncatedCount)
            putCount("breadcrumbsRemoved", breadcrumbsRemovedCount)
            putCount("breadcrumbBytesRemoved", breadcrumbBytesRemovedCount)
        }

        return HashMap<String, Any>().apply {
            put("remoteConfig", remoteConfigEnabled)

            if (configDifferences.isNotEmpty()) {
                put("config", configDifferences)
            }

            if (callbacks.isNotEmpty()) {
                put("callbacks", callbacks)
            }

            if (system.isNotEmpty()) {
                put("system", system)
            }
        }
    }

    override fun setConfigDifferences(differences: Map<String, Any>) {
        configDifferences.clear()
        configDifferences.putAll(differences)
        // This is currently the only place where we set static data.
        // When that changes in future, we'll need a StaticData object to properly merge data
        // coming from multiple sources.
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

    override fun setMetadataTrimMetrics(stringsTrimmed: Int, charsRemoved: Int) {
        metadataStringsTrimmedCount = stringsTrimmed
        metadataCharsTruncatedCount = charsRemoved
    }

    override fun setBreadcrumbTrimMetrics(breadcrumbsRemoved: Int, bytesRemoved: Int) {
        breadcrumbsRemovedCount = breadcrumbsRemoved
        breadcrumbBytesRemovedCount = bytesRemoved
    }

    private fun MutableMap<String, Any>.putCount(name: String, value: Int) {
        if (value > 0) {
            put(name, value)
        }
    }

    private fun Map<String, Any>.getInt(name: String): Int {
        val value = get(name) as? Number
        return value?.toInt() ?: 0
    }
}
