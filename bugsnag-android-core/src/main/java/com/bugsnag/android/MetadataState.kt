package com.bugsnag.android

internal data class MetadataState(val metadata: Metadata = Metadata()) : BaseObservable(),
    MetadataAware {

    override fun addMetadata(section: String, value: Any?) = addMetadata(section, null, value)
    override fun addMetadata(section: String, key: String?, value: Any?) {
        metadata.addMetadata(section, key, value)

        when (value) {
            null -> notifyClear(key, section)
            else -> notifyObservers(StateEvent.AddMetadata(section, key, value))
        }
    }

    override fun clearMetadata(section: String) = clearMetadata(section, null)
    override fun clearMetadata(section: String, key: String?) {
        metadata.clearMetadata(section, key)
        notifyClear(key, section)
    }

    private fun notifyClear(key: String?, section: String) {
        when (key) {
            null -> notifyObservers(StateEvent.ClearMetadataTab(section))
            else -> notifyObservers(StateEvent.RemoveMetadata(section, key))
        }
    }

    override fun getMetadata(section: String) = getMetadata(section, null)
    override fun getMetadata(section: String, key: String?) = metadata.getMetadata(section, key)
}
