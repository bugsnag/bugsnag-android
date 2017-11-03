package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class SessionTrackingPayload implements JsonStream.Streamable {

    private final Notifier notifier;
    private final Collection<Session> sessions;
    private final DeviceDataSummary deviceDataSummary = new DeviceDataSummary();

    SessionTrackingPayload(Collection<Session> sessions) {
        this.notifier = Notifier.getInstance();
        this.sessions = new ArrayList<>();
        this.sessions.addAll(sessions);
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        writer.name("notifier").value(notifier);

        // TODO serialize app, device
//        writer.name("app");
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
