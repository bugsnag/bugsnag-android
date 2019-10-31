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
    internal fun loadPlugins(
        client: Client,
        immutableConfig: ImmutableConfig,
        logger: Logger
    ) {
        plugins
            .toSet()
            .mapNotNull { convertClzToPlugin(it) }
            .forEach { it.initialisePlugin(client) }

        if (immutableConfig.autoDetectNdkCrashes) {
            try {
                registerPlugin(Class.forName("com.bugsnag.android.NdkPlugin"))
                logger.i("Registering NDK plugin")
            } catch (exc: ClassNotFoundException) {
                logger.w("bugsnag-plugin-android-ndk artefact not found on classpath, "
                        + "NDK errors will not be captured.")
            }
        }
        if (immutableConfig.autoDetectAnrs) {
            try {
                registerPlugin(Class.forName("com.bugsnag.android.AnrPlugin"))
                logger.i("Registering ANR plugin")
            } catch (exc: ClassNotFoundException) {
                logger.w("bugsnag-plugin-android-anr artefact not found on classpath, "
                        + "ANR errors will not be captured.")
            }
        }
    }

    private fun convertClzToPlugin(it: Class<*>): BugsnagPlugin? {
        return try {
            it.newInstance() as BugsnagPlugin
        } catch (exc: Exception) {
            null
        }
    }

    fun createAnrEvent(exc: Throwable, client: Client) =
        Event(exc, client.immutableConfig, HandledState.newInstance(HandledState.REASON_ANR))
}
