package com.bugsnag.android;

import com.bugsnag.android.AppWithState;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.Delivery;
import com.bugsnag.android.DeliveryParams;
import com.bugsnag.android.DeliveryStatus;
import com.bugsnag.android.DeviceBuildInfo;
import com.bugsnag.android.DeviceWithState;
import com.bugsnag.android.ImmutableConfig;
import com.bugsnag.android.ImmutableConfigKt;
import com.bugsnag.android.NoopLogger;
import com.bugsnag.android.Report;
import com.bugsnag.android.SessionPayload;

import androidx.annotation.NonNull;

import java.util.Date;

public class JavaHooks {

    /**
     * Generates fake DeviceWithState
     */
    @NonNull
    public static DeviceWithState generateDeviceWithState() {
        DeviceBuildInfo buildInfo = DeviceBuildInfo.Companion.defaultInfo();
        return new DeviceWithState(buildInfo, new String[]{}, null, null, null,
                109230923452L, 22234423124L, 92340255592L, "portrait", new Date(0));
    }

    /**
     * Generates fake AppWithState
     */
    @NonNull
    public static AppWithState generateAppWithState() {
        return new AppWithState(generateImmutableConfig(), null, null, null,
                null, null, null, null);
    }

    /**
     * Generates fake Configuration
     */
    @NonNull
    public static Configuration generateConfiguration() {
        Configuration configuration = new Configuration("5d1ec5bd39a74caa1267142706a7fb21");
        configuration.setDelivery(generateDelivery());
        configuration.setLogger(NoopLogger.INSTANCE);
        return configuration;
    }

    /**
     * Generates fake Delivery
     */
    @NonNull
    public static Delivery generateDelivery() {
        return new Delivery() {
            @NonNull
            @Override
            public DeliveryStatus deliver(@NonNull Report report,
                                          @NonNull DeliveryParams deliveryParams) {
                return DeliveryStatus.DELIVERED;
            }

            @NonNull
            @Override
            public DeliveryStatus deliver(@NonNull SessionPayload payload,
                                          @NonNull DeliveryParams deliveryParams) {
                return DeliveryStatus.DELIVERED;
            }
        };
    }

    /**
     * Generates fake ImmutableConfig
     */
    @NonNull
    public static ImmutableConfig generateImmutableConfig() {
        return ImmutableConfigKt.convertToImmutableConfig(generateConfiguration());
    }
}
