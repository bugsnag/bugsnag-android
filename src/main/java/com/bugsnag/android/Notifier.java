package com.bugsnag.android;

class Notifier implements JsonStream.Streamable {
    static final String NOTIFIER_NAME = "Android Bugsnag Notifier";
    static final String NOTIFIER_VERSION = "3.0.0";
    static final String NOTIFIER_URL = "https://bugsnag.com";

    private static Notifier instance = new Notifier();
    public static Notifier getInstance() {
        return instance;
    }

    public void toStream(JsonStream writer) {
        writer.beginObject()
            .name("name").value(NOTIFIER_NAME)
            .name("version").value(NOTIFIER_VERSION)
            .name("url").value(NOTIFIER_URL)
        .endObject();
    }
}
