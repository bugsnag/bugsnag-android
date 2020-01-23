package com.bugsnag.android;

import androidx.annotation.NonNull;

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

import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class JavaHooks {

    public static DeviceWithState generateDeviceWithState() {
        DeviceBuildInfo buildInfo = DeviceBuildInfo.Companion.defaultInfo();
        return new DeviceWithState(buildInfo, new String[]{}, null, null, null,
                109230923452L, 22234423124L, 92340255592L, "portrait", new Date(0));
    }

    public static AppWithState generateAppWithState() {
        return new AppWithState(generateImmutableConfig(), null, null, null,
                null, null, null, null);
    }

    public static Configuration generateConfiguration() {
        Configuration configuration = new Configuration("5d1ec5bd39a74caa1267142706a7fb21");
        configuration.setDelivery(generateDelivery());
        configuration.setLogger(NoopLogger.INSTANCE);
        return configuration;
    }


    public static Delivery generateDelivery() {
        return new Delivery() {
            @NotNull
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

    public static ImmutableConfig generateImmutableConfig() {
        return ImmutableConfigKt.convertToImmutableConfig(generateConfiguration());
    }
}
