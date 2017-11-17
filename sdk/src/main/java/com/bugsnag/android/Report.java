package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * An error report payload.
 * <p>
 * This payload contains an error report and identifies the source application
 * using your API key.
 */
public class Report implements JsonStream.Streamable {
    @Nullable
    private final File errorFile;
    @Nullable
    private Error error;
    private String apiKey;
    private Notifier notifier;

    Report(@NonNull String apiKey, @Nullable File errorFile) {
        this.apiKey = apiKey;
        this.error = null;
        this.errorFile = errorFile;
        this.notifier = Notifier.getInstance();
    }

    Report(@NonNull String apiKey, @Nullable Error error) {
        this.apiKey = apiKey;
        this.error = error;
        this.errorFile = null;
        this.notifier = Notifier.getInstance();
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        // Create a JSON stream and top-level object
        writer.beginObject();

        // Write the API key
        writer.name("apiKey").value(apiKey);

        // Write the notifier info
        writer.name("notifier").value(notifier);

        // Start events array
        writer.name("events").beginArray();

        // Write in-memory event
        if (error != null) {
            writer.value(error);
        }

        // Write on-disk event
        if (errorFile != null) {
            writer.value(errorFile);
        }

        // End events array
        writer.endArray();

        // End the main JSON object
        writer.endObject();
    }

    @Nullable
    public Error getError() {
        return error;
    }

    public void setApiKey(@NonNull String apiKey) {
        this.apiKey = apiKey;
    }

    public void setNotifierVersion(@NonNull String version) {
        notifier.setVersion(version);
    }

    public void setNotifierName(@NonNull String name) {
        notifier.setName(name);
    }

    public void setNotifierURL(@NonNull String url) {
        notifier.setURL(url);
    }
}
