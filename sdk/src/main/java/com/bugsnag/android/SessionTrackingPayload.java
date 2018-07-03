package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SessionTrackingPayload implements JsonStream.Streamable {

    private final Notifier notifier;
    private final Session session;
    private final DeviceDataSummary deviceDataSummary = new DeviceDataSummary();
    private final Map<String, Object> appData;
    private final List<File> files;

    SessionTrackingPayload(List<File> files, Map<String, Object> appData) {
        this.appData = appData;
        this.notifier = Notifier.getInstance();
        this.session = null;
        this.files = files;
    }

    SessionTrackingPayload(Session session, Map<String, Object> appDataSummary) {
        this.appData = appDataSummary;
        this.notifier = Notifier.getInstance();
        this.session = session;
        this.files = null;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        writer.name("notifier").value(notifier);
        writer.name("app").value(appData);
        writer.name("device").value(deviceDataSummary);
        writer.name("sessions").beginArray();

        if (session == null) {
            for (File file : files) {
                writer.value(file);
            }
        } else {
            writer.value(session);
        }

        writer.endArray();
        writer.endObject();
    }

}
