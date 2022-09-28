package com.bugsnag.android;

import com.bugsnag.android.internal.ImmutableConfig;

import kotlin.LazyKt;
import kotlin.jvm.functions.Function0;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

class TestData {
    static ImmutableConfig generateConfig() throws IOException {
        Configuration config = new Configuration("123456abcdeabcde");
        config.setLogger(null);
        Delivery delivery = new DefaultDelivery(null, config);
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
                EnumSet.of(Telemetry.INTERNAL_ERRORS, Telemetry.USAGE),
                "production",
                "builduuid-123",
                "1.4.3",
                55,
                "android",
                delivery,
                new EndpointConfiguration(),
                true,
                55,
                NoopLogger.INSTANCE,
                22,
                32,
                32,
                1000,
                10000,
                LazyKt.lazy(new Function0<File>() {
                    @Override
                    public File invoke() {
                        try {
                            return Files.createTempDirectory("foo").toFile();
                        } catch (IOException ignored) {
                            return null;
                        }
                    }
                }),
                true,
                null,
                null,
                Collections.singleton("password")
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
                new Stacktrace(frames),
                ErrorType.REACTNATIVEJS
        );
        return new Error(impl, NoopLogger.INSTANCE);
    }
}
