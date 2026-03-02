package com.bugsnag.android

/**
 * Installs an NDK based [OutOfMemoryHandler] into the client. OutOfMemoryErrors will be redirected
 * to the NDK crash reporter, where they can be reported without allocating more Android Runtime
 * memory. To use this plugin, add it to your Bugsnag Configuration:
 *
 * ```kotlin
 * configuration.addPlugin(NativeOutOfMemoryPlugin())
 * Bugsnag.start(this, configuration)
 * ```
 *
 * This makes reporting of `OutOfMemoryError`s significantly more reliable, but comes with one
 * current caveat: thread stack traces will not be captured or reported (this may change in the
 * future).
 *
 * This plugin is useful if you are receiving `OutOfMemoryError` reports that do not include their
 * stack traces, as these are typically a result of error reporters attempting to allocate the
 * error report itself. This plugin side-steps the problem by using pre-allocated native
 * memory to capture and store the `OutOfMemoryError` for delivery on restart.
 */
class NativeOutOfMemoryPlugin : Plugin, OutOfMemoryHandler {
    private var previousOomHandler: OutOfMemoryHandler? = null
    private var client: Client? = null
    private var ndkPlugin: NdkPlugin? = null

    override fun load(client: Client) {
        this.client = client
        ndkPlugin = client.getPlugin(NdkPlugin::class.java) as? NdkPlugin

        this.previousOomHandler = client.outOfMemoryHandler
        client.outOfMemoryHandler = this
    }

    override fun unload() {
        client?.outOfMemoryHandler = previousOomHandler
        previousOomHandler = null
        client = null
    }

    override fun onOutOfMemory(oom: OutOfMemoryError): Boolean {
        ndkPlugin?.nativeBridge?.reportOutOfMemory(oom)
        // consume the OutOfMemoryError
        return true
    }
}
