package com.bugsnag.android;

import java.util.Collections;
import java.util.HashSet;

class TestData {
    static ImmutableConfig generateConfig() {
        return new ImmutableConfig(
                "123456abcdeabcde",
                true,
                new ErrorTypes(),
                true,
                ThreadSendPolicy.ALWAYS,
                Collections.singleton("com.example.DiscardClass"),
                Collections.singleton("production"),
                Collections.singleton("com.example"),
                new HashSet<>(Collections.singletonList(BreadcrumbType.MANUAL)),
                "production",
                "builduuid-123",
                "1.4.3",
                55,
                "code-id-123",
                "android",
                new DefaultDelivery(null, NoopLogger.INSTANCE),
                new EndpointConfiguration(),
                true,
                55,
                NoopLogger.INSTANCE,
                22
        );
    }
}
