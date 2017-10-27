package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * The severity of an Error, one of "error", "warning" or "info".
 * <p>
 * By default, unhandled exceptions will be Severity.ERROR and handled
 * exceptions sent with bugsnag.notify will be Severity.WARNING.
 */
public enum Severity implements JsonStream.Streamable {
    ERROR("error"),
    WARNING("warning"),
    INFO("info");

    private final String name;

    Severity(String name) {
        this.name = name;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.value(name);
    }

    static Severity fromString(String s) {
        switch (s) {
            case "error":
                return ERROR;
            case "warning":
                return WARNING;
            case "info":
                return INFO;
            default:
                return null;
        }
    }

    public String getName() {
        return name;
    }
}
