package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;

public class SessionTrackingPayload implements JsonStream.Streamable {

    private Notifier notifier;

    SessionTrackingPayload() {
        this.notifier = Notifier.getInstance();
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        writer.name("notifier").value(notifier);

        // TODO serialize app, device
        writer.name("app");
        writer.name("device");

        writer.endObject();

    }

}
