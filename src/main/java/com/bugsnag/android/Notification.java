package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * An error notification payload.
 *
 * This payload contains an error report and identifies the source application
 * using your API key.
 */
class Notification implements JsonStream.Streamable {
    private final Configuration config;
    private final Collection<Error> errors;
    private final Collection<File> errorFiles;

    Notification(@NonNull Configuration config) {
        this.config = config;
        this.errors = new LinkedList<Error>();
        this.errorFiles = new LinkedList<File>();
    }

    public void toStream(@NonNull JsonStream writer) throws IOException {
        // Create a JSON stream and top-level object
        writer.beginObject();

            // Write the API key
            writer.name("apiKey").value(config.apiKey);

            // Write the notifier info
            writer.name("notifier").value(Notifier.getInstance());

            // Start events array
            writer.name("events").beginArray();

            // Write any in-memory events
            for(Error error : errors) {
                writer.value(error);
            }

            // Write any on-disk events
            for(File errorFile : errorFiles) {
                writer.value(errorFile);
            }

            // End events array
            writer.endArray();

        // End the main JSON object
        writer.endObject();
    }

    void addError(@NonNull Error error) {
        this.errors.add(error);
    }

    void addError(@NonNull File errorFile) {
        this.errorFiles.add(errorFile);
    }

    int deliver() throws HttpClient.NetworkException, HttpClient.BadResponseException {
        HttpClient.post(config.getNotifyEndpoint(), this);
        return errors.size() + errorFiles.size();
    }
}
