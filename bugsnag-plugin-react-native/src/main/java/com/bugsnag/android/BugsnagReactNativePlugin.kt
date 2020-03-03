package com.bugsnag.android

import java.util.Locale

/**
 * The BugsnagReactNativePlugin is invoked by the BugsnagReactNative class in the bugsnag-js repo.
 * Its responsibility is to update the native client when informed of state changes in the JS layer
 * (e.g. when the user is set) and vice versa. It is also responsible for dispatching JS errors to
 * the native client and for providing metadata information (such as threads) to the JS layer.
 */
class BugsnagReactNativePlugin : Plugin {

    private val configSerializer = ConfigSerializer()
    private val appSerializer = AppSerializer()
    private val deviceSerializer = DeviceSerializer()
    private val breadcrumbSerializer = BreadcrumbSerializer()
    private val threadSerializer = ThreadSerializer()

    internal lateinit var internalHooks: InternalHooks
    internal lateinit var client: Client

    lateinit var logger: Logger

    override fun load(client: Client) {
        client.logger.i("Initialized React Native Plugin")
    }

    override fun unload() {}

    @Suppress("unused")
    fun configure(): Map<String, Any?> {
        // see if bugsnag-android is already initialised
        client = Bugsnag.getClient()
        logger = client.logger
        internalHooks = InternalHooks(client)

        client.registerObserver(BugsnagReactNativeBridge(client) {
            // TODO future: serialize event to JS layer
            logger.d("React native event: $it")
        })

        // TODO: I think we also want to return values for state here too:
        // i.e of user, context and metadata
        val map = HashMap<String, Any?>()
        configSerializer.serialize(map, internalHooks.config)
        return map
    }

    fun leaveBreadcrumb(map: Map<String, Any?>?) {
        requireNotNull(map)
        val msg = map["message"] as String
        val type = BreadcrumbType.valueOf((map["type"] as String).toUpperCase(Locale.US))
        val obj = map["metadata"] ?: emptyMap<String, Any>()
        @Suppress("UNCHECKED_CAST")
        client.leaveBreadcrumb(msg, type, obj as Map<String, Any>)
    }

    fun startSession() {
        client.startSession()
    }

    fun pauseSession() {
        client.pauseSession()
    }

    fun resumeSession() {
        client.resumeSession()
    }

    fun updateContext(context: String?) {
        client.context = context
    }

    fun updateMetadata(section: String, data: Map<String, Any?>?) {
        when (data) {
            null -> client.clearMetadata(section)
            else -> client.addMetadata(section, data)
        }
    }

    fun updateUser(id: String?, email: String?, name: String?) {
        client.setUser(id, email, name)
    }

    @Suppress("unused")
    fun dispatch(@Suppress("UNUSED_PARAMETER") payload: Map<String, Any?>?) {
        // TODO implement
    }

    @Suppress("unused")
    fun getPayloadInfo(unhandled: Boolean): Map<String, Any?> {
        val info = mutableMapOf<String, Any?>()
        val app = mutableMapOf<String, Any?>()
        appSerializer.serialize(app, internalHooks.appWithState)
        info["app"] = app

        val device = mutableMapOf<String, Any?>()
        deviceSerializer.serialize(device, internalHooks.deviceWithState)
        info["device"] = device

        info["breadcrumbs"] = internalHooks.breadcrumbs.map {
            val map = mutableMapOf<String, Any?>()
            breadcrumbSerializer.serialize(map, it)
            map
        }
        info["threads"] = internalHooks.getThreads(unhandled).map {
            val map = mutableMapOf<String, Any?>()
            threadSerializer.serialize(map, it)
            map
        }
        return info
    }
}
