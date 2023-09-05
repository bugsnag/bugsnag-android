package com.bugsnag.android

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

    private var ignoreJsExceptionCallbackAdded = false

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
        observerBridge = BugsnagReactNativeBridge(client) {
            jsCallback?.invoke(it)
        }
        client.addObserver(observerBridge)
        client.logger.i("Initialized React Native Plugin")
    }

    private fun updateNotifierInfo(env: Map<String, Any?>) {
        val reactNativeVersion = env["reactNativeVersion"] as String?
        reactNativeVersion?.let { client.addRuntimeVersionInfo("reactNative", it) }

        val engine = env["engine"] as String?
        engine?.let { client.addRuntimeVersionInfo("reactNativeJsEngine", it) }

        val jsVersion = env["notifierVersion"] as String
        val notifier = client.notifier
        notifier.name = "Bugsnag React Native"
        notifier.url = "https://github.com/bugsnag/bugsnag-js"
        notifier.version = jsVersion
        notifier.dependencies = listOf(Notifier()) // depend on bugsnag-android
    }

    private fun ignoreJavaScriptExceptions() {
        ignoreJsExceptionCallbackAdded = true
        this.client.addOnError(
            OnErrorCallback { event ->
                event.errors[0].errorClass != "com.facebook.react.common.JavascriptException"
            }
        )
    }

    override fun unload() {}

    @Suppress("unused")
    fun configure(env: Map<String, Any?>?): Map<String, Any?> {
        requireNotNull(env)
        updateNotifierInfo(env)

        // JS exceptions are caught by the JS layer and passed to the dispatch method
        // in this plugin. When a JS exception crashes the app, we get a duplicate
        // native exception for the same error so we ignore those once the JS layer
        // has started.
        if (!ignoreJsExceptionCallbackAdded) { ignoreJavaScriptExceptions() }

        val map = HashMap<String, Any?>()
        configSerializer.serialize(map, client.config)
        return map
    }

    fun registerForMessageEvents(cb: (MessageEvent) -> Unit) {
        this.jsCallback = cb
        client.syncInitialState()
    }

    fun leaveBreadcrumb(map: Map<String, Any?>?) {
        requireNotNull(map)
        val msg = map["message"] as String
        val type = BreadcrumbType.valueOf((map["type"] as String).uppercase())
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

    fun addFeatureFlag(name: String, variant: String?) {
        client.addFeatureFlag(name, variant)
    }

    fun clearFeatureFlag(name: String) {
        client.clearFeatureFlag(name)
    }

    fun clearFeatureFlags() {
        client.clearFeatureFlags()
    }

    fun clearMetadata(section: String, key: String?) {
        when (key) {
            null -> client.clearMetadata(section)
            else -> client.clearMetadata(section, key)
        }
    }

    fun addMetadata(section: String, data: Map<String, Any?>?) {
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
        val projectPackages = internalHooks.getProjectPackages(client.config)
        val event = EventDeserializer(client, projectPackages).deserialize(payload)

        if (event.errors.isEmpty()) {
            return
        }
        val errorClass = event.errors[0].errorClass
        if (client.immutableConfig.shouldDiscardError(errorClass)) {
            return
        }
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

        info["breadcrumbs"] = client.breadcrumbs.map {
            val map = mutableMapOf<String, Any?>()
            breadcrumbSerializer.serialize(map, it)
            map
        }
        info["threads"] = internalHooks.getThreads(unhandled).map {
            val map = mutableMapOf<String, Any?>()
            threadSerializer.serialize(map, it)
            map
        }
        info["appMetadata"] = internalHooks.getAppMetadata()
        info["deviceMetadata"] = internalHooks.getDeviceMetadata()
        return info
    }
}
