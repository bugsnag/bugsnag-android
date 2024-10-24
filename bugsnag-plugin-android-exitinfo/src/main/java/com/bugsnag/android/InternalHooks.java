package com.bugsnag.android;

import static com.bugsnag.android.Bugsnag.client;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

class InternalHooks {

    private InternalHooks() {
    }

    public static void setEventStoreEmptyCallback(Client client, Function0<Unit> callback) {
        client.eventStore.setOnEventStoreEmptyCallback(callback);
    }

    public static void deliver(Client client, Event event) {
        client.deliveryDelegate.deliver(event);
    }


    public static Event createEmptyANR(Long exitInfoTimeStamp) {
        Event event = NativeInterface.createEmptyEvent();
        event.setDevice(client.deviceDataCollector
                .generateEmptyEventDeviceWithState(exitInfoTimeStamp));
        event.setApp(client.appDataCollector.generateEmptyEventAppWithState());
        event.updateSeverityReason(SeverityReason.REASON_ANR);
        return event;
    }

    public static Event createEmptyCrash() {
        Event event = NativeInterface.createEmptyEvent();
        event.updateSeverityReason(SeverityReason.REASON_ANR);
        return event;
    }

}
