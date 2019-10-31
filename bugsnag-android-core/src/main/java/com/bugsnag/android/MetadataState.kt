package com.bugsnag.android

internal data class MetadataState(val metadata: Metadata = Metadata()) : MetadataAware {

    override fun addMetadata(section: String, value: Any?) = addMetadata(section, null, value)
    override fun addMetadata(section: String, key: String?, value: Any?) =
        metadata.addMetadata(section, key, value)

    override fun clearMetadata(section: String) = clearMetadata(section, null)
    override fun clearMetadata(section: String, key: String?) = metadata.clearMetadata(section, key)

    override fun getMetadata(section: String) = getMetadata(section, null)
    override fun getMetadata(section: String, key: String?): Any? =
        metadata.getMetadata(section, key)

    fun copy() = this.copy(metadata = metadata)
}
