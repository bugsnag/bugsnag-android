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

    private lateinit var observerBridge: BugsnagReactNativeBridge
    var jsCallback: ((event: MessageEvent) -> Unit)? = null

    override fun load(client: Client) {
        this.client = client
        logger = client.logger
        internalHooks = InternalHooks(client)

        // register a state observer immediately but only pass events on when JS callback set
        client.registerObserver(observerBridge)
        observerBridge = BugsnagReactNativeBridge(client) {
            jsCallback?.invoke(it)
        }
        client.logger.i("Initialized React Native Plugin")
    }

    private fun updateNotifierInfo(env: Map<String, Any?>) {
        val reactNativeVersion = env["reactNativeVersion"] as String?
        reactNativeVersion?.let { client.addRuntimeVersionInfo("reactNativeVersion", it) }

        val engine = env["engine"] as String?
        engine?.let { client.addRuntimeVersionInfo("engine", it) }

        val jsVersion = env["notifierVersion"] as String
        val notifier = client.notifier
        notifier.name = "Bugsnag React Native"
        notifier.url = "https://github.com/bugsnag/bugsnag-js"
        notifier.version = jsVersion
        notifier.dependencies.add(Notifier()) // depend on bugsnag-android
    }

    override fun unload() {}

    @Suppress("unused")
    fun configure(env: Map<String, Any?>?): Map<String, Any?> {
        requireNotNull(env)
        updateNotifierInfo(env)

        val map = HashMap<String, Any?>()
        configSerializer.serialize(map, internalHooks.config)
        return map
    }

    fun registerForMessageEvents(cb: (MessageEvent) -> Unit) {
        this.jsCallback = cb
        client.syncInitialState()
    }

    fun leaveBreadcrumb(map: Map<String, Any?>?) {
        requireNotNull(map)
        val msg = map["message"] as String
        val type = BreadcrumbType.valueOf((map["type"] as String).toUpperCase(Locale.US))
        val obj = map["metadata"] ?: emptyMap<String, Any>()
        @Suppress("UNCHECKED_CAST")
        client.leaveBreadcrumb(msg, obj as Map<String, Any>, type)
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

    fun updateCodeBundleId(id: String?) {
        client.codeBundleId = id
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
    fun dispatch(payload: MutableMap<String, Any?>?) {
        requireNotNull(payload)
        val event = EventDeserializer(client).deserialize(payload)
        client.notifyInternal(event, null)
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
