package com.bugsnag.android;

import java.io.File;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.LinkedList;

class Notification implements JsonStream.Streamable {
    private Configuration config;
    private Collection<Error> errors;
    private Collection<File> errorFiles;

    Notification(Configuration config) {
        this.config = config;
        this.errors = new LinkedList<Error>();
        this.errorFiles = new LinkedList<File>();
    }

    public void toStream(JsonStream writer) {
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

    void addError(Error error) {
        this.errors.add(error);
    }

    void addError(File errorFile) {
        this.errorFiles.add(errorFile);
    }

    int deliver() throws HttpClient.NetworkException, HttpClient.BadResponseException {
        HttpClient.post(config.getNotifyEndpoint(), this);
        return errors.size() + errorFiles.size();
    }
}
