package com.bugsnag.android;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

class InternalHooks {

    public InternalHooks() {}

    public static void setEventStoreEmptyCallback(Client client, Function0<Unit> callback) {
        client.eventStore.setOnEventStoreEmptyCallback(callback);
    }
}
