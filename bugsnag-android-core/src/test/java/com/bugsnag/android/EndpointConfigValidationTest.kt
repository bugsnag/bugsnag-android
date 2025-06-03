package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test

class EndpointConfigValidationTest {

    private val _defaultNotify = "https://notify.bugsnag.com"
    private val _defaultSession = "https://sessions.bugsnag.com"
    private val _hubNotify = "https://notify.insighthub.smartbear.com"
    private val _hubSession = "https://sessions.insighthub.smartbear.com"

    /** No custom endpoints*/
    @Test
    fun defaultEndpoints() {
        val config = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        assertEquals(_defaultNotify, config.endpoints.notify)
        assertEquals(_defaultSession, config.endpoints.sessions)
    }

    /** No custom endpoints with a Hub key*/
    @Test
    fun defaultEndpointsForHubKey() {
        val config = Configuration("00000bd39a74caa1267142706a7fb21")
        assertEquals(_hubNotify, config.endpoints.notify)
        assertEquals(_hubSession, config.endpoints.sessions)
    }

    /** Both endpoints overridden → custom values are honoured verbatim */
    @Test
    fun customEndpointsProvided() {
        val config = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        config.endpoints = EndpointConfiguration(
            "https://notify.example.com",
            "https://sessions.example.com"
        )
        assertEquals("https://notify.example.com", config.endpoints.notify)
        assertEquals("https://sessions.example.com", config.endpoints.sessions)
    }
}
