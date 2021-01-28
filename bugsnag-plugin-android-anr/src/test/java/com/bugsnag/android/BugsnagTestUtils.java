package com.bugsnag.android;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

final class BugsnagTestUtils {

    private BugsnagTestUtils() {
    }

    static Configuration generateConfiguration() {
        Configuration configuration = new Configuration("5d1ec5bd39a74caa1267142706a7fb21");
        configuration.setDelivery(generateDelivery());
        configuration.setLogger(NoopLogger.INSTANCE);
        try {
            File dir = Files.createTempDirectory("test").toFile();
            configuration.setPersistenceDirectory(dir);
        } catch (IOException ignored) {
            // ignore IO exception
        }
        return configuration;
    }

    static ImmutableConfig generateImmutableConfig() {
        return convert(generateConfiguration());
    }

    static MetadataState generateMetadataState() {
        return new MetadataState();
    }

    static ImmutableConfig convert(Configuration config) {
        return ImmutableConfigKt.convertToImmutableConfig(config, null);
    }

    public static Delivery generateDelivery() {
        return new Delivery() {
            @NotNull
            @Override
            public DeliveryStatus deliver(@NotNull EventPayload payload,
                                          @NotNull DeliveryParams deliveryParams) {
                return DeliveryStatus.DELIVERED;
            }

            @NotNull
            @Override
            public DeliveryStatus deliver(@NotNull Session payload,
                                          @NotNull DeliveryParams deliveryParams) {
                return DeliveryStatus.DELIVERED;
            }
        };
    }
}
