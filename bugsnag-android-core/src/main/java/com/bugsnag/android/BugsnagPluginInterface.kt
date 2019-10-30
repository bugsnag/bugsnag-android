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

    fun createAnrEvent(exc: Throwable, client: Client) =
        Event(exc, client.config, HandledState.newInstance(HandledState.REASON_ANR))
}
