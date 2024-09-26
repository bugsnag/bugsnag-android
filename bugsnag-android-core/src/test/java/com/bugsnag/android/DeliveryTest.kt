package com.bugsnag.android

import com.bugsnag.android.internal.DeliveryHelper.getDeliveryStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DeliveryTest {

    @Test
    fun testResponseCodeMapping() {
        assertEquals(DeliveryStatus.DELIVERED, getDeliveryStatus(202))
        assertEquals(DeliveryStatus.UNDELIVERED, getDeliveryStatus(503))
        assertEquals(DeliveryStatus.UNDELIVERED, getDeliveryStatus(0))
        assertEquals(DeliveryStatus.UNDELIVERED, getDeliveryStatus(408))
        assertEquals(DeliveryStatus.UNDELIVERED, getDeliveryStatus(429))
        assertEquals(DeliveryStatus.FAILURE, getDeliveryStatus(400))
        assertEquals(DeliveryStatus.FAILURE, getDeliveryStatus(401))
        assertEquals(DeliveryStatus.FAILURE, getDeliveryStatus(498))
        assertEquals(DeliveryStatus.FAILURE, getDeliveryStatus(499))
        assertEquals(DeliveryStatus.UNDELIVERED, getDeliveryStatus(408))
        assertEquals(DeliveryStatus.UNDELIVERED, getDeliveryStatus(429))
    }
}
