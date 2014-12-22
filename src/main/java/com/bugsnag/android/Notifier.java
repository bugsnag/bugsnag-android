package com.bugsnag.android;

/**
 * Information about this library, including name and version.
 */
class Notifier implements JsonStream.Streamable {
    static final String NOTIFIER_NAME = "Android Bugsnag Notifier";
    static final String NOTIFIER_VERSION = "3.1.1";
    static final String NOTIFIER_URL = "https://bugsnag.com";

    private static Notifier instance = new Notifier();
    public static Notifier getInstance() {
        return instance;
    }

    public void toStream(JsonStream writer) {
        writer.beginObject();
            writer.name("name").value(NOTIFIER_NAME);
            writer.name("version").value(NOTIFIER_VERSION);
            writer.name("url").value(NOTIFIER_URL);
        writer.endObject();
    }
}
