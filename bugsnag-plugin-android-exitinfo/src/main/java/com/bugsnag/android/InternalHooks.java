package com.bugsnag.android;

import static com.bugsnag.android.Bugsnag.client;

import androidx.annotation.NonNull;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

class InternalHooks {

    private InternalHooks() {
    }

    public static void setEventStoreEmptyCallback(Client client, Function0<Unit> callback) {
        client.eventStore.setOnEventStoreEmptyCallback(callback);
    }

    static void deliver(@NonNull Client client, @NonNull Event event) {
        client.deliveryDelegate.deliver(event);
    }

    static Event createEmptyANR(long exitInfoTimeStamp) {
        Event event = NativeInterface.createEmptyEvent();
        event.setDevice(client.deviceDataCollector
                .generateHistoricDeviceWithState(exitInfoTimeStamp));
        event.setApp(client.appDataCollector.generateHistoricAppWithState());
        event.updateSeverityReason(SeverityReason.REASON_ANR);
        return event;
    }

    static Event createEmptyCrash(long exitInfoTimeStamp) {
        Event event = NativeInterface.createEmptyEvent();
        event.setDevice(client.deviceDataCollector
                .generateHistoricDeviceWithState(exitInfoTimeStamp));
        event.setApp(client.appDataCollector.generateHistoricAppWithState());
        event.updateSeverityReason(SeverityReason.REASON_SIGNAL);
        return event;
    }

}
