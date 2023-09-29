package com.bugsnag.android

import android.content.Context
import androidx.startup.Initializer

class BugsnagInitializer : Initializer<Client> {
    override fun create(context: Context): Client = Bugsnag.start(context)
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
