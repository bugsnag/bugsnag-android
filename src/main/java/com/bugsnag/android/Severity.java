package com.bugsnag.android;

public enum Severity implements JsonStream.Streamable {
    ERROR   ("error"),
    WARNING ("warning"),
    INFO    ("info");

    private final String name;

    Severity(String name) {
        this.name = name;
    }

    public void toStream(JsonStream writer) {
        writer.value(name);
    }
}
