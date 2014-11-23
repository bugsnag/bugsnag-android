package com.bugsnag.android;

public enum Severity implements JsonStream.Streamable {
    ERROR, WARNING, INFO;

    public void toStream(JsonStream writer) {
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
