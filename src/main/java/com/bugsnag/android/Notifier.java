package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * Information about this library, including name and version.
 */
class Notifier implements JsonStream.Streamable {
    static final String NOTIFIER_NAME = "Android Bugsnag Notifier";
    static final String NOTIFIER_VERSION = "3.4.0";
    static final String NOTIFIER_URL = "https://bugsnag.com";

    private static final Notifier instance = new Notifier();
    public static Notifier getInstance() {
        return instance;
    }

    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
            writer.name("name").value(NOTIFIER_NAME);
            writer.name("version").value(NOTIFIER_VERSION);
            writer.name("url").value(NOTIFIER_URL);
        writer.endObject();
    }
}
