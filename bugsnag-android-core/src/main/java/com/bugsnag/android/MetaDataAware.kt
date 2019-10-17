package com.bugsnag.android

internal interface MetaDataAware {
    fun addMetadata(section: String, key: String?, value: Any?)
    fun clearMetadata(section: String, key: String?)
    fun getMetadata(section: String, key: String?): Any?
}
