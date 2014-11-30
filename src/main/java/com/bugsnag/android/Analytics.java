package com.bugsnag.android;

import java.io.Writer;
import java.io.OutputStreamWriter;

class Analytics implements JsonStream.Streamable {
    private Configuration config;
    private Diagnostics diagnostics;
    private User user;

    Analytics(Configuration config, Diagnostics diagnostics, User user) {
        this.config = config;
        this.diagnostics = diagnostics;
        this.user = user;
    }

    public void toStream(JsonStream writer) {
        // Create a JSON stream and top-level object
        writer.beginObject();

            // Write the API key
            writer.name("apiKey").value(config.apiKey);

            // Write the notifier info
            writer.name("notifier").value(Notifier.getInstance());

            // Write diagnostics
            writer.name("app").value(diagnostics.getAppData());
            writer.name("device").value(diagnostics.getDeviceData());

            // Write user info
            writer.name("user").value(user);

        // End the main JSON object
        writer.endObject().close();
    }

    void deliver() throws java.io.IOException {
        HttpClient.post(config.getAnalyticsEndpoint(), this);
    }
}
