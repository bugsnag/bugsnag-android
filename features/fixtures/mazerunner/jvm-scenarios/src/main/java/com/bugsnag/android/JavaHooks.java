package com.bugsnag.android;

import com.bugsnag.android.internal.ImmutableConfig;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Date;

public class JavaHooks {

    /**
     * Generates fake DeviceWithState
     */
    @NonNull
    public static DeviceWithState generateDeviceWithState() {
        DeviceBuildInfo buildInfo = DeviceBuildInfo.Companion.defaultInfo();
        return new DeviceWithState(buildInfo, null, null, null,
                109230923452L, Collections.<String, Object>emptyMap(), 22234423124L,
                92340255592L, "portrait", new Date(0));
    }

    /**
     * Generates fake AppWithState
     */
    @NonNull
    public static AppWithState generateAppWithState(ImmutableConfig config) {
        return new AppWithState(config, null, null, null,
                null, null, null, null, null, null);
    }

    /**
     * Generates fake Delivery
     */
    @NonNull
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

    @NonNull
    public static Delivery createDefaultDelivery() {
        return new DefaultDelivery(null, "test-api-key", 10000, NoopLogger.INSTANCE);
    }
}
