package com.bugsnag.android;

import com.facebook.infer.annotation.ThreadSafe;

@ThreadSafe
enum DeliveryStyle {
    SAME_THREAD,
    ASYNC,
    ASYNC_WITH_CACHE
}
