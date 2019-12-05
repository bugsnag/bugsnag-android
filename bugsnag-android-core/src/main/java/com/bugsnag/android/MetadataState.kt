package com.bugsnag.android

internal data class MetadataState(val metadata: Metadata = Metadata()) : BaseObservable(),
    MetadataAware {

    override fun addMetadata(section: String, value: Map<String, Any?>) = metadata.addMetadata(section, value)
    override fun addMetadata(section: String, key: String, value: Any?) {
        metadata.addMetadata(section, key, value)

        when (value) {
            null -> notifyClear(section, key)
            else -> notifyObservers(StateEvent.AddMetadata(section, key, metadata.getMetadata(section, key)))
        }
    }

    override fun clearMetadata(section: String) {
        metadata.clearMetadata(section)
        notifyClear(section, null)
    }
    override fun clearMetadata(section: String, key: String) {
        metadata.clearMetadata(section, key)
        notifyClear(section, key)
    }

    private fun notifyClear(section: String, key: String?) {
        when (key) {
            null -> notifyObservers(StateEvent.ClearMetadataTab(section))
            else -> notifyObservers(StateEvent.RemoveMetadata(section, key))
        }
    }

    override fun getMetadata(section: String) = metadata.getMetadata(section)
    override fun getMetadata(section: String, key: String) = metadata.getMetadata(section, key)
}
