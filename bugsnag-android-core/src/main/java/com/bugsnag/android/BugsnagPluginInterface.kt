package com.bugsnag.android

interface BugsnagPlugin {
    fun initialisePlugin(client: Client)
}

object BugsnagPluginInterface {

    private val plugins = mutableSetOf<Class<*>>()

    fun registerPlugin(clz: Class<*>) {
        plugins.add(clz)
    }

    @JvmName("loadPlugins")
    internal fun loadPlugins(client: Client) {
        plugins
            .toSet()
            .mapNotNull { convertClzToPlugin(it) }
            .forEach { it.initialisePlugin(client) }
    }

    private fun convertClzToPlugin(it: Class<*>): BugsnagPlugin? {
        return try {
            it.newInstance() as BugsnagPlugin
        } catch (exc: Exception) {
            null
        }
    }

    /**
     * Constructs an event from an exception, by accessing the internally visible constructor
     * and returning the created object. This API is intended for internal use only.
     */
    fun createEvent(exc: Throwable, client: Client, severityReason: String): Event =
        Event(exc, client.config, HandledState.newInstance(severityReason))
}
