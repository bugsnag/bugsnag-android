package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.NetworkException

abstract internal class Scenario {

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

}
