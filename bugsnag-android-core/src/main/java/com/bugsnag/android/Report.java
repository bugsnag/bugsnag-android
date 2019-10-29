package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * An error report payload.
 * <p>
 * This payload contains an error report and identifies the source application
 * using your API key.
 */
public final class Report implements JsonStream.Streamable {

    @Nullable
    private final File eventFile;

    @Nullable
    private final Event event;

    @NonNull
    private final Notifier notifier;

    @NonNull
    private String apiKey;

    Report(@NonNull String apiKey, @NonNull Event event) {
        this(apiKey, null, event);
    }

    Report(@NonNull String apiKey, @Nullable File eventFile) {
        this(apiKey, eventFile, null);
    }

    private Report(@NonNull String apiKey, @Nullable File eventFile, @Nullable Event event) {
        this.event = event;
        this.eventFile = eventFile;
        this.notifier = Notifier.INSTANCE;
        this.apiKey = apiKey;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        // Create a JSON stream and top-level object
        writer.beginObject();

        writer.name("apiKey").value(apiKey);
        writer.name("payloadVersion").value("4.0");

        // Write the notifier info
        writer.name("notifier").value(notifier);

        // Start events array
        writer.name("events").beginArray();

        // Write in-memory event
        if (event != null) {
            writer.value(event);
        } else if (eventFile != null) { // Write on-disk event
            writer.value(eventFile);
        } else {
            throw new IOException("Expected event or eventFile");
        }

        // End events array
        writer.endArray();

        // End the main JSON object
        writer.endObject();
    }

    @NonNull
    public Event getEvent() {
        return event;
    }

    /**
     * Alters the API key used for this error report.
     *
     * @param apiKey the new API key
     */
    public void setApiKey(@NonNull String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * @return the API key sent as part of this report.
     */
    @NonNull
    public String getApiKey() {
        return apiKey;
    }

    @NonNull
    public Notifier getNotifier() {
        return notifier;
    }
}
