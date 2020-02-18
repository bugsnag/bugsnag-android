package com.bugsnag.android

/**
 * Stateful information set by the notifier about your app can be found on this class. These values
 * can be accessed and amended if necessary.
 */
class AppWithState internal constructor(
    config: ImmutableConfig,
    binaryArch: String?,
    id: String?,
    releaseStage: String?,
    version: String?,

    /**
     * The number of milliseconds the application was running before the event occurred
     */
    var duration: Number?,

    /**
     * The number of milliseconds the application was running in the foreground before the
     * event occurred
     */
    var durationInForeground: Number?,

    /**
     * Whether the application was in the foreground when the event occurred
     */
    var inForeground: Boolean?
) : App(config, binaryArch, id, releaseStage, version) {

    override fun serialiseFields(writer: JsonStream) {
        super.serialiseFields(writer)
        writer.name("duration").value(duration)
        writer.name("durationInForeground").value(durationInForeground)
        writer.name("inForeground").value(inForeground)
    }

}
