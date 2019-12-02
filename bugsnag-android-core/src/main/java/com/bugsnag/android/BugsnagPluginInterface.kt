package com.bugsnag.android

interface BugsnagPlugin {
    fun loadPlugin(client: Client)
    fun unloadPlugin()
    var loaded: Boolean
}

object BugsnagPluginInterface {

    private var plugins = mutableMapOf<Class<*>, BugsnagPlugin>()
    private var registeredPluginClasses = mutableSetOf<Class<*>>()

    fun registerPlugin(clz: Class<*>) {
        registeredPluginClasses.add(clz)
    }

    fun loadRegisteredPlugins(client: Client) {
        registeredPluginClasses.forEach { loadPlugin(client, it) }
    }

    @JvmName("loadPlugin")
    internal fun loadPlugin(client: Client, clz: Class<*>) {
        var plugin = plugins[clz]

        if (plugin == null) { // attempt to instantiate the plugin
            plugin = try {
                clz.newInstance() as BugsnagPlugin
            } catch (exc: Exception) {
                null
            }
        }
        if (plugin != null && !plugin.loaded) {
            plugins[clz] = plugin
            plugin.loadPlugin(client)
            plugin.loaded = true
        }
    }

    @JvmName("unloadPlugin")
    internal fun unloadPlugin(clz: Class<*>) {
        val plugin = plugins[clz]

        if (plugin != null && plugin.loaded) {
            plugin.unloadPlugin()
            plugin.loaded = false
        }
    }
}
