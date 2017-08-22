package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * The severity of an Error, one of "error", "warning" or "info".
 *
 * By default, unhandled exceptions will be Severity.ERROR and handled
 * exceptions sent with bugsnag.notify will be Severity.WARNING.
 */
public enum Severity implements JsonStream.Streamable {
    ERROR   ("error"),
    WARNING ("warning"),
    INFO    ("info");

    private final String name;

    Severity(String name) {
        this.name = name;
    }

    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.value(name);
    }
}
