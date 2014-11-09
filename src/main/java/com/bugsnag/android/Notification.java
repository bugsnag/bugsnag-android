package com.bugsnag.android;

import java.io.File;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.LinkedList;

class Notification implements HttpClient.Streamable {
    private Configuration config;
    private List<Error> errors;
    private List<File> errorFiles;

    Notification(Configuration config) {
        this.config = config;
        this.errors = new LinkedList<Error>();
        this.errorFiles = new LinkedList<File>();
    }

    public void toStream(Writer out) {
        // Create a JSON stream and top-level object
        JsonStream writer = new JsonStream(out).beginObject();

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
        writer.endObject().close();
    }

    void addError(Error error) {
        this.errors.add(error);
    }

    void addError(File errorFile) {
        this.errorFiles.add(errorFile);
    }

    void print() {
        // Write the notification to System.out
        toStream(new OutputStreamWriter(System.out));

        // Flush System.out
        System.out.println();
    }

    int deliver() throws java.io.IOException {
        HttpClient.post(config.getNotifyEndpoint(), this);
        return errors.size() + errorFiles.size();
    }
}
