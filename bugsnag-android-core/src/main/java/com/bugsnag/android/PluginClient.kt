package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig

internal class PluginClient(
    userPlugins: Set<Plugin>,
    private val immutableConfig: ImmutableConfig,
    private val logger: Logger
) {

    companion object {
        private const val NDK_PLUGIN = "com.bugsnag.android.NdkPlugin"
        private const val ANR_PLUGIN = "com.bugsnag.android.AnrPlugin"
        private const val RN_PLUGIN = "com.bugsnag.android.BugsnagReactNativePlugin"
    }

    private val ndkPlugin = instantiatePlugin(NDK_PLUGIN)
    private val anrPlugin = instantiatePlugin(ANR_PLUGIN)
    private val rnPlugin = instantiatePlugin(RN_PLUGIN)

    private val plugins: Set<Plugin> = userPlugins.toSet()
    private val internalPlugins = setOf(ndkPlugin, anrPlugin, rnPlugin)

    private fun instantiatePlugin(clz: String): Plugin? {
        return try {
            val pluginClz = Class.forName(clz)
            pluginClz.newInstance() as Plugin
        } catch (exc: ClassNotFoundException) {
            logger.d("Plugin '$clz' is not on the classpath - functionality will not be enabled.")
            null
        } catch (exc: Throwable) {
            logger.e("Failed to load plugin '$clz'", exc)
            null
        }
    }

    /**
     * Loads the ANR plugin
     */
    fun loadAnrPlugin(client: Client) {
        anrPlugin?.let {
            loadPluginInternal(it, client)
        }
    }

    /**
     * Loads the NDK plugin
     */
    fun loadNdkPlugin(client: Client) {
        ndkPlugin?.let {
            loadPluginInternal(it, client)
        }
    }

    /**
     * Loads the React Native plugin
     */
    fun loadReactNativePlugin(client: Client) {
        rnPlugin?.let {
            loadPluginInternal(it, client)
        }
    }

    /**
     * Loads any plugins added by the user
     */
    fun loadUserPlugins(client: Client) {
        plugins.forEach {
            loadPluginInternal(it, client)
        }
    }

    fun setAutoNotify(client: Client, autoNotify: Boolean) {
        setAutoDetectAnrs(client, autoNotify)

        if (autoNotify) {
            ndkPlugin?.load(client)
        } else {
            ndkPlugin?.unload()
        }
    }

    fun setAutoDetectAnrs(client: Client, autoDetectAnrs: Boolean) {
        if (autoDetectAnrs) {
            anrPlugin?.load(client)
        } else {
            anrPlugin?.unload()
        }
    }

    fun findPlugin(clz: Class<*>): Plugin? {
        val objs = plugins.plus(internalPlugins).filterNotNull()
        return objs.find { it.javaClass == clz }
    }

    private fun loadPluginInternal(plugin: Plugin, client: Client) {
        try {
            val name = plugin.javaClass.name
            val errorTypes = immutableConfig.enabledErrorTypes

            // only initialize NDK/ANR plugins if automatic detection enabled
            if (name == NDK_PLUGIN) {
                if (errorTypes.ndkCrashes) {
                    plugin.load(client)
                }
            } else if (name == ANR_PLUGIN) {
                if (errorTypes.anrs) {
                    plugin.load(client)
                }
            } else {
                plugin.load(client)
            }
        } catch (exc: Throwable) {
            logger.e("Failed to load plugin $plugin, continuing with initialisation.", exc)
        }
    }
}
