package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

class InternalHooks {

    private InternalHooks() {
    }

    public static void setEventStoreEmptyCallback(Client client, Function0<Unit> callback) {
        client.eventStore.setOnEventStoreEmptyCallback(callback);
    }

    public static void setDiscardEventCallback(
            Client client,
            Function1<EventPayload, Unit> callback) {
        client.eventStore.setOnDiscardEventCallback(callback);
    }

    static void deliver(@NonNull Client client, @NonNull Event event) {
        client.deliveryDelegate.deliver(event);
    }

    @Nullable
    static Event createEmptyANR(long exitInfoTimeStamp) {
        try {
            Client client = Bugsnag.getClient();
            DeviceDataCollector deviceDataCollector = client.getDeviceDataCollector();

            if (deviceDataCollector == null) {
                return null;
            }

            AppDataCollector appDataCollector = client.getAppDataCollector();
            if (appDataCollector == null) {
                return null;
            }

            Event event = NativeInterface.createEmptyEvent();
            event.setDevice(deviceDataCollector.generateHistoricDeviceWithState(exitInfoTimeStamp));
            event.setApp(appDataCollector.generateHistoricAppWithState());
            event.updateSeverityReason(SeverityReason.REASON_ANR);
            return event;
        } catch (Exception ex) {
            return null;
        }
    }

    @Nullable
    static Event createEmptyCrash(long exitInfoTimeStamp) {
        try {
            Client client = Bugsnag.getClient();
            DeviceDataCollector deviceDataCollector = client.getDeviceDataCollector();

            if (deviceDataCollector == null) {
                return null;
            }

            AppDataCollector appDataCollector = client.getAppDataCollector();
            if (appDataCollector == null) {
                return null;
            }

            Event event = NativeInterface.createEmptyEvent();
            event.setDevice(deviceDataCollector.generateHistoricDeviceWithState(exitInfoTimeStamp));
            event.setApp(appDataCollector.generateHistoricAppWithState());
            event.updateSeverityReason(SeverityReason.REASON_SIGNAL);
            return event;
        } catch (Exception ex) {
            return null;
        }
    }
}
