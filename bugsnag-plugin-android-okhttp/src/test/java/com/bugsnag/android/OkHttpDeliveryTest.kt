package com.bugsnag.android

import com.bugsnag.android.okhttp.NoopLogger
import com.bugsnag.android.okhttp.OkHttpDelivery
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

class OkHttpDeliveryTest {

    @Test
    fun testResponseCodeMapping() {
        val delivery = OkHttpDelivery(OkHttpClient.Builder().build(), "myApiKey", 10000, NoopLogger)
        assertEquals(DeliveryStatus.DELIVERED, delivery.getDeliveryStatus(202))
        assertEquals(DeliveryStatus.UNDELIVERED, delivery.getDeliveryStatus(503))
        assertEquals(DeliveryStatus.UNDELIVERED, delivery.getDeliveryStatus(0))
        assertEquals(DeliveryStatus.UNDELIVERED, delivery.getDeliveryStatus(408))
        assertEquals(DeliveryStatus.UNDELIVERED, delivery.getDeliveryStatus(429))
        assertEquals(DeliveryStatus.FAILURE, delivery.getDeliveryStatus(400))
        assertEquals(DeliveryStatus.FAILURE, delivery.getDeliveryStatus(401))
        assertEquals(DeliveryStatus.FAILURE, delivery.getDeliveryStatus(498))
        assertEquals(DeliveryStatus.FAILURE, delivery.getDeliveryStatus(499))
        assertEquals(DeliveryStatus.UNDELIVERED, delivery.getDeliveryStatus(408))
        assertEquals(DeliveryStatus.UNDELIVERED, delivery.getDeliveryStatus(429))
    }
}
