package com.bugsnag.android;

import org.jetbrains.annotations.NotNull;

final class BugsnagTestUtils {

    private BugsnagTestUtils() {
    }

    static ConfigInternal generateConfiguration() {
        ConfigInternal configuration = new ConfigInternal("5d1ec5bd39a74caa1267142706a7fb21");
        configuration.setDelivery(generateDelivery());
        configuration.setLogger(NoopLogger.INSTANCE);
        return configuration;
    }

    static ImmutableConfig generateImmutableConfig() {
        return convert(generateConfiguration());
    }


    static ImmutableConfig convert(ConfigInternal config) {
        return ImmutableConfigKt.convertToImmutableConfig(config);
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
