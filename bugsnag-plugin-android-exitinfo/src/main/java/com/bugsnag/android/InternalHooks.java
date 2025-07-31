package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

class InternalHooks {

    private final Client client;

    InternalHooks(Client client) {
        this.client = client;
    }

    void setEventStoreEmptyCallback(Function0<Unit> callback) {
        client.getEventStore().setOnEventStoreEmptyCallback(callback);
    }

    void setDiscardEventCallback(Function1<EventPayload, Unit> callback) {
        client.getEventStore().setOnDiscardEventCallback(callback);
    }

    void deliver(@NonNull Event event) {
        client.deliveryDelegate.deliver(event);
    }

    @Nullable
    Event createEmptyANR(long exitInfoTimeStamp) {
        try {
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
    Event createEmptyCrash(long exitInfoTimeStamp) {
        try {
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
