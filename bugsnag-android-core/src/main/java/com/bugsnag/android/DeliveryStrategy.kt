package com.bugsnag.android

enum class DeliveryStrategy {
    STORE_ONLY,
    STORE_AND_FLUSH,
    STORE_AND_SEND,
    SEND_IMMEDIATELY,
}
