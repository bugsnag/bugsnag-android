package com.bugsnag.android;

@ThreadSafe
enum DeliveryStyle {
    SAME_THREAD,
    ASYNC,
    ASYNC_WITH_CACHE
}
