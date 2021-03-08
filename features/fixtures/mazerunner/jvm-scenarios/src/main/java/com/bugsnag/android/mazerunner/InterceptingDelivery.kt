package com.bugsnag.android.mazerunner

import com.bugsnag.android.Delivery
import com.bugsnag.android.DeliveryParams
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Session

class InterceptingDelivery(
    private val baseDelivery: Delivery,
    private val callback: (response: DeliveryStatus) -> Unit
) : Delivery {

    override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
        val response = baseDelivery.deliver(payload, deliveryParams)
        callback(response)
        return response
    }

    override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
        val response = baseDelivery.deliver(payload, deliveryParams)
        callback(response)
        return response
    }
}
