package com.bugsnag.android

interface BugsnagConfiguration {
    fun setContext(context: String?)
    fun getContext(): String?
    fun setMetaData(metaData: MetaData)
    fun getMetaData(): MetaData
    fun addBeforeNotify(beforeNotify: BeforeNotify)
}
