package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * Information about this library, including name and version.
 */
public class Notifier implements JsonStream.Streamable {

    private static final String NOTIFIER_NAME = "Android Bugsnag Notifier";
    private static final String NOTIFIER_VERSION = "4.6.0";
    private static final String NOTIFIER_URL = "https://bugsnag.com";

    @NonNull
    private String name = NOTIFIER_NAME;

    @NonNull
    private String version = NOTIFIER_VERSION;

    @NonNull
    private String url = NOTIFIER_URL;

    private static final Notifier instance = new Notifier();

    @NonNull
    public static Notifier getInstance() {
        return instance;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        writer.name("name").value(name);
        writer.name("version").value(version);
        writer.name("url").value(url);
        writer.endObject();
    }

    @InternalApi
    public void setVersion(@NonNull String version) {
        this.version = version;
    }

    @InternalApi
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    public void setURL(@NonNull String url) {
        this.url = url;
    }

    @InternalApi
    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    @InternalApi
    public String getName() {
        return name;
    }

    @NonNull
    @InternalApi
    public String getVersion() {
        return version;
    }

    @NonNull
    @InternalApi
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    public String getURL() {
        return url;
    }
}
