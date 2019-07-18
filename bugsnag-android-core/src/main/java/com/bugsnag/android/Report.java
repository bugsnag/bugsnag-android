package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * An error report payload.
 * <p>
 * This payload contains an error report and identifies the source application
 * using your API key.
 */
public class Report implements JsonStream.Streamable {

    @NonNull
    private final Error error;

    @NonNull
    private final Notifier notifier;

    @NonNull
    private String apiKey;
    private transient boolean cachingDisabled;

    Report(@NonNull String apiKey, @NonNull Error error) {
        this.error = error;
        this.notifier = Notifier.getInstance();
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
        writer.value(error);

        // End events array
        writer.endArray();

        // End the main JSON object
        writer.endObject();
    }

    @NonNull
    public Error getError() {
        return error;
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

    @InternalApi
    @NonNull
    public Notifier getNotifier() {
        return notifier;
    }

    boolean isCachingDisabled() {
        return cachingDisabled;
    }

    void setCachingDisabled(boolean cachingDisabled) {
        this.cachingDisabled = cachingDisabled;
    }
}
