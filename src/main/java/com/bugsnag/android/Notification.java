package com.bugsnag.android;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.LinkedList;

class Notification {
    private Configuration config;
    private List<Error> errors;
    private List<File> errorFiles;

    public Notification(Configuration config) {
        this.config = config;
        this.errors = new LinkedList<Error>();
    }

    public void addError(Error error) {
        this.errors.add(error);
    }

    public void addError(File errorFile) {
        this.errorFiles.add(errorFile);
    }

    public void toStream(JsonStreamer writer) {
        // Write apiKey and notifier information
        writer
            .name("apiKey").value(config.apiKey)
            .name("notifier").beginObject()
                .name("name").value(Configuration.NOTIFIER_NAME)
                .name("version").value(Configuration.NOTIFIER_VERSION)
                .name("url").value(Configuration.NOTIFIER_URL)
            .endObject();

        // Write errors
        writer.name("events").beginArray();
        for(Error error : errors) {
            writer.value(error);
        }

        for(File errorFile : errorFiles) {
            // TODO: How to handle file streams with JsonWriter?
        }
        writer.endArray();
    }

    public void deliver(HttpClient.ResponseHandler handler) {
        // TODO: Create a JsonWriter, pass the stream to HttpClient.post
        InputStream bodyStream = null;
        HttpClient.post(config.getNotifyEndpoint(), bodyStream, handler);
    }
}
