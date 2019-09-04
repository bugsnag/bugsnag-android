package com.bugsnag.android;

import org.jetbrains.annotations.NotNull;

final class BugsnagTestUtils {

    private BugsnagTestUtils() {
    }

    static Configuration generateConfiguration() {
        Configuration configuration = new Configuration("test");
        configuration.setDelivery(generateDelivery());
        return configuration;
    }

    static ImmutableConfig generateImmutableConfig() {
        return convert(generateConfiguration());
    }


    static ImmutableConfig convert(Configuration config) {
        return ImmutableConfigKt.convertToImmutableConfig(config);
    }

    public static Delivery generateDelivery() {
        return new Delivery() {
            @NotNull
            @Override
            public DeliveryStatus deliver(@NotNull Report report,
                                          @NotNull DeliveryParams deliveryParams) {
                return DeliveryStatus.DELIVERED;
            }

            @NotNull
            @Override
            public DeliveryStatus deliver(@NotNull SessionTrackingPayload payload,
                                          @NotNull DeliveryParams deliveryParams) {
                return DeliveryStatus.DELIVERED;
            }
        };
    }
}
