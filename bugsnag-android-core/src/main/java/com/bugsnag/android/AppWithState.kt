package com.bugsnag.android

class AppWithState internal constructor(
    config: ImmutableConfig,
    binaryArch: String?,
    id: String?,
    releaseStage: String?,
    version: String?,
    var duration: Number?,
    var durationInForeground: Number?,
    var inForeground: Boolean?
) : App(config, binaryArch, id, releaseStage, version) {

    override fun serialiseFields(writer: JsonStream) {
        super.serialiseFields(writer)
        writer.name("duration").value(duration)
        writer.name("durationInForeground").value(durationInForeground)
        writer.name("inForeground").value(inForeground)
    }

}
