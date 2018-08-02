package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SessionTrackingPayload implements JsonStream.Streamable {

    private final Notifier notifier;
    private final Session session;
    private final Map<String, Object> deviceDataSummary;
    private final Map<String, Object> appDataSummary;
    private final List<File> files;

    SessionTrackingPayload(Session session,
                           List<File> files,
                           AppData appData,
                           DeviceData deviceData) {
        this.appDataSummary = appData.getAppDataSummary();
        this.deviceDataSummary = deviceData.getDeviceDataSummary();
        this.notifier = Notifier.getInstance();
        this.session = session;
        this.files = files;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        writer.name("notifier").value(notifier);
        writer.name("app").value(appDataSummary);
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
