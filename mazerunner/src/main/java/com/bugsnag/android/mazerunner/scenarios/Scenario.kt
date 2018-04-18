package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Client
import com.bugsnag.android.Configuration
import com.bugsnag.android.NetworkException

abstract internal class Scenario(protected val config: Configuration,
                                 protected val context: Context) {

    var eventMetaData: String? = null

    open fun run() {
        Bugsnag.init(context, config)
        Bugsnag.setLoggingEnabled(true)
    }

    /**
     * Returns a throwable with the message as the current classname
     */
    protected fun generateException(): Throwable = RuntimeException(javaClass.simpleName)

}
