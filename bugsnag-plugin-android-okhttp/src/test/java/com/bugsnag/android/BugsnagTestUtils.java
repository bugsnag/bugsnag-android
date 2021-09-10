package com.bugsnag.android;

import com.bugsnag.android.internal.ImmutableConfig;
import com.bugsnag.android.internal.ImmutableConfigKt;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

final class BugsnagTestUtils {

    static ImmutableConfig generateImmutableConfig() {
        Configuration config = new Configuration("5d1ec5bd39a74caa1267142706a7fb21");
        return generateImmutableConfig(config);
    }

    static ImmutableConfig generateImmutableConfig(Configuration config) {
        config.setDelivery(generateDelivery());
        config.setLogger(NoopLogger.INSTANCE);
        config.setProjectPackages(Collections.singleton("com.example.foo"));
        try {
            File dir = Files.createTempDirectory("test").toFile();
            config.setPersistenceDirectory(dir);
        } catch (IOException ignored) {
            // ignore IO exception
        }
        return ImmutableConfigKt.convertToImmutableConfig(config);
    }

    public static Delivery generateDelivery() {
        return new Delivery() {
            @NonNull
            @Override
            public DeliveryStatus deliver(@NonNull EventPayload payload,
                                          @NonNull DeliveryParams deliveryParams) {
                return DeliveryStatus.DELIVERED;
            }

            @NonNull
            @Override
            public DeliveryStatus deliver(@NonNull Session payload,
                                          @NonNull DeliveryParams deliveryParams) {
                return DeliveryStatus.DELIVERED;
            }
        };
    }
}
