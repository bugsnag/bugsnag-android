package com.bugsnag.android;

public enum Severity implements JsonStreamer.Streamable {
    ERROR, WARNING, INFO;

    public void toStream(JsonStreamer writer) {
        switch(this) {
            case ERROR:
                writer.value("error");
                break;
            case WARNING:
                writer.value("warning");
                break;
            case INFO:
                writer.value("info");
                break;
        }
    }
}
