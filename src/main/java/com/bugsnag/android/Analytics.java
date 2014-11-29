package com.bugsnag.android;

import java.io.Writer;
import java.io.OutputStreamWriter;

class Analytics implements HttpClient.Streamable {
    private Configuration config;
    private Diagnostics diagnostics;

    Analytics(Configuration config, Diagnostics diagnostics) {
        this.config = config;
        this.diagnostics = diagnostics;
    }

    public void toStream(Writer out) {
        // Create a JSON stream and top-level object
        JsonStream writer = new JsonStream(out).beginObject();

            // Write the API key
            writer.name("apiKey").value(config.apiKey);

            // Write the notifier info
            writer.name("notifier").value(Notifier.getInstance());

            // Write diagnostics
            writer.name("app").value(diagnostics.getAppData());
            writer.name("device").value(diagnostics.getDeviceData());

            // TODO: Write user info

        // End the main JSON object
        writer.endObject().close();
    }

    void print() {
        // Write the notification to System.out
        toStream(new OutputStreamWriter(System.out));

        // Flush System.out
        System.out.println();
    }

    void deliver() throws java.io.IOException {
        HttpClient.post(config.getAnalyticsEndpoint(), this);
    }
}
