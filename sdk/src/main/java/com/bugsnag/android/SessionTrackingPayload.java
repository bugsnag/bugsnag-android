package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class SessionTrackingPayload implements JsonStream.Streamable {

    private final File payloadFile;
    private final Notifier notifier;
    private final Collection<Session> sessions;
    private final DeviceDataSummary deviceDataSummary = new DeviceDataSummary();
    private final AppDataSummary appDataSummary;

    SessionTrackingPayload(File payloadFile) {
        this.payloadFile = payloadFile;
        this.appDataSummary = null;
        this.notifier = null;
        this.sessions = null;
    }

    SessionTrackingPayload(Collection<Session> sessions, AppData appDataSummary) {
        this.appDataSummary = appDataSummary;
        this.notifier = Notifier.getInstance();
        this.sessions = new ArrayList<>();
        this.sessions.addAll(sessions);
        this.payloadFile = null;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        if (payloadFile != null) {
            writer.value(payloadFile);
        } else {
            writer.beginObject();
            writer.name("notifier").value(notifier);
            writer.name("app").value(appDataSummary);
            writer.name("device").value(deviceDataSummary);
            writer.name("sessions").beginArray();

            for (Session session : sessions) {
                writer.beginObject()
                    .name("id").value(session.getId())
                    .name("startedAt").value(DateUtils.toISO8601(session.getStartedAt()));

                User user = session.getUser();

                if (user != null) {
                    writer.name("user").value(user);
                }
                writer.endObject();
            }

            writer.endArray();
            writer.endObject();
        }
    }

}
