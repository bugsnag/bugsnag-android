package com.bugsnag.android;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

class InternalHooks {

    private InternalHooks() {}

    public static void setEventStoreEmptyCallback(Client client, Function0<Unit> callback) {
        client.eventStore.setOnEventStoreEmptyCallback(callback);
    }
}
