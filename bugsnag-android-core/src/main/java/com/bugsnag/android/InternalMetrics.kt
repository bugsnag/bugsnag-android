package com.bugsnag.android

/**
 * Stores internal metrics for Bugsnag use.
 */
internal class InternalMetrics(
    private val configDifferences: Map<String, Any>,
    private val callbackCounts: Map<String, Int>
) {
    fun toJsonableMap(): Map<String, Any> {
        val callbacks = mutableMapOf<String, Any>()
        addCurrentCallbackSetCounts(callbacks)
        addCurrentNativeApiCallUsage(callbacks)
        return mapOf(
            "config" to configDifferences,
            "callbacks" to callbacks
        )
    }

    private fun addCurrentCallbackSetCounts(callbacks: MutableMap<String, Any>) {
        val countsIndexNdkOnError = 0

        callbacks.putAll(callbackCounts)
        @Suppress("UNCHECKED_CAST")
        val counts = invokeNdkPlugin("getCurrentCallbackSetCounts") as List<Long>?
        if (counts != null && !counts.isEmpty()) {
            // ndkOnError comes from the native side. The rest we already have.
            callbacks["ndkOnError"] = counts[countsIndexNdkOnError]
        }
    }

    private fun addCurrentNativeApiCallUsage(callbacks: MutableMap<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val usage = invokeNdkPlugin("getCurrentNativeApiCallUsage") as List<String>?
        if (usage != null) {
            for (api in usage) {
                callbacks[api] = true
            }
        }
    }

    private fun invokeNdkPlugin(methodName: String, vararg args: Any): Any? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val clz = Class.forName("com.bugsnag.android.NdkPlugin") as Class<Plugin>
            val plugin = Bugsnag.getClient().getPlugin(clz)
            if (plugin != null) {
                val method = plugin.javaClass.getMethod(methodName)
                method.invoke(plugin, *args)
            } else {
                null
            }
        } catch (exc: Exception) {
            null
        }
    }
}
