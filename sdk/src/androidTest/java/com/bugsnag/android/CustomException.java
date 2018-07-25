package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;

class CustomException extends Exception implements JsonStream.Streamable {

    private static final long serialVersionUID = 1962801050513549513L;

    CustomException(String message) {
        super(message);
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        writer.name("errorClass").value("CustomizedException");
        writer.name("message").value(getLocalizedMessage());
        writer.name("stacktrace");
        writer.beginArray();

        writer.beginObject();
        writer.name("file").value("MyFile.java");
        writer.name("lineNumber").value(408);
        writer.name("offset").value(18);
        writer.name("method").value("MyFile.run");
        writer.endObject();

        writer.endArray();
        writer.endObject();
    }
}
