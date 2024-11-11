package com.bugsnag.android;

import com.bugsnag.android.Client;
import com.bugsnag.android.Event;
import com.bugsnag.android.EventStore;
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
        return new DefaultDelivery(null, NoopLogger.INSTANCE);
    }

    /**
     * Trigger an internal bugsnag error
     */

    @NonNull
    public static void triggerInternalBugsnagForError(Client client) {
        client.getEventStore().write((stream) -> {
            throw new IllegalStateException("Mazerunner threw exception serializing error");
        });
    }

    @NonNull
    public static void flushErrorStoreAsync(Client client) {
        client.getEventStore().flushAsync();
    }

    @NonNull
    public static void flushErrorStoreOnLaunch(Client client) {
        client.getEventStore().flushOnLaunch();
    }

    @NonNull
    public static void writeErrorToStore(Client client, Event event) {
        client.getEventStore().write(event);
    }
}
