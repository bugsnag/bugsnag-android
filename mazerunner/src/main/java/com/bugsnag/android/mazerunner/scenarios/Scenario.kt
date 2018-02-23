package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.NetworkException

abstract internal class Scenario {

    var context: Context? = null

    abstract fun run()

    /**
     * Sets a NOP implementation for the Session Tracking API, preventing delivery
     */
    protected fun disableSessionDelivery() {
        Bugsnag.setSessionTrackingApiClient({ _, _, _ ->
            throw NetworkException("Session Delivery NOP", RuntimeException("NOP"))
        })
    }

    /**
     * Sets a NOP implementation for the Error Tracking API, preventing delivery
     */
    protected fun disableReportDelivery() {
        Bugsnag.setErrorReportApiClient({ _, _, _ ->
            throw NetworkException("Error Delivery NOP", RuntimeException("NOP"))
        })
    }

    /**
     * Sets a NOP implementation for the Error Tracking API and the Session Tracking API,
     * preventing delivery
     */
    protected fun disableAllDelivery() {
        disableSessionDelivery()
        disableReportDelivery()
    }

    /**
     * Returns a throwable with the message as the current classname
     */
    protected fun generateException(): Throwable = RuntimeException(javaClass.simpleName)

}
