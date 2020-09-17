package com.bugsnag.android;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

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
                "android",
                new DefaultDelivery(null, NoopLogger.INSTANCE),
                new EndpointConfiguration(),
                true,
                55,
                NoopLogger.INSTANCE,
                22
        );
    }

    static Error generateError() {
        List<Stackframe> frames = Collections.singletonList(new Stackframe(
                "foo",
                "Bar.kt",
                5,
                true
        ));

        ErrorInternal impl = new ErrorInternal(
                "BrowserException",
                "whoops!",
                new Stacktrace(frames, NoopLogger.INSTANCE),
                ErrorType.REACTNATIVEJS
        );
        return new Error(impl, NoopLogger.INSTANCE);
    }
}
