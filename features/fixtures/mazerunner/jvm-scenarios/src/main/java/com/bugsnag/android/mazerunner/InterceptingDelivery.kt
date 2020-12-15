package com.bugsnag.android.mazerunner

import com.bugsnag.android.*

class InterceptingDelivery(private val baseDelivery: Delivery,
                           private val callback: () -> Unit): Delivery {

    override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
        val response = baseDelivery.deliver(payload, deliveryParams)
        callback()
        return response
    }

    override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
        val response =  baseDelivery.deliver(payload, deliveryParams)
        callback()
        return response
    }
}