package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryStatusTest {

    @Test
    fun testResponseCodeMapping() {
        assertEquals(DeliveryStatus.DELIVERED, DeliveryStatus.forHttpResponseCode(202))
        assertEquals(DeliveryStatus.UNDELIVERED, DeliveryStatus.forHttpResponseCode(503))
        assertEquals(DeliveryStatus.UNDELIVERED, DeliveryStatus.forHttpResponseCode(0))
        assertEquals(DeliveryStatus.UNDELIVERED, DeliveryStatus.forHttpResponseCode(408))
        assertEquals(DeliveryStatus.UNDELIVERED, DeliveryStatus.forHttpResponseCode(429))
        assertEquals(DeliveryStatus.FAILURE, DeliveryStatus.forHttpResponseCode(400))
        assertEquals(DeliveryStatus.FAILURE, DeliveryStatus.forHttpResponseCode(401))
        assertEquals(DeliveryStatus.FAILURE, DeliveryStatus.forHttpResponseCode(498))
        assertEquals(DeliveryStatus.FAILURE, DeliveryStatus.forHttpResponseCode(499))
        assertEquals(DeliveryStatus.UNDELIVERED, DeliveryStatus.forHttpResponseCode(408))
        assertEquals(DeliveryStatus.UNDELIVERED, DeliveryStatus.forHttpResponseCode(429))
    }
}
