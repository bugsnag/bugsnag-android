package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SessionTrackingPayload implements JsonStream.Streamable {

    private final Notifier notifier;
    private final Collection<Session> sessions;
    private final DeviceDataSummary deviceDataSummary = new DeviceDataSummary();
    private final AppDataSummary appDataSummary;
    private final List<File> files;

    SessionTrackingPayload(List<File> files, AppData appDataSummary) {
        this.appDataSummary = appDataSummary;
        this.notifier = Notifier.getInstance();
        this.sessions = null;
        this.files = files;
    }
    SessionTrackingPayload(Collection<Session> sessions, AppData appDataSummary) {
        this.appDataSummary = appDataSummary;
        this.notifier = Notifier.getInstance();
        this.sessions = new ArrayList<>();
        this.sessions.addAll(sessions);
        this.files = null;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        writer.name("notifier").value(notifier);
        writer.name("app").value(appDataSummary);
        writer.name("device").value(deviceDataSummary);

        writer.name("sessions").beginArray();

        if (sessions == null) {
            for (File file : files) {
                writer.value(file);
            }
        } else {
            for (Session session : sessions) {
                writer.value(session);
            }
        }

        writer.endArray();
        writer.endObject();
    }

    Collection<Session> getSessions() {
        return sessions;
    }
}
