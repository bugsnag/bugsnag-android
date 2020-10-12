package com.bugsnag.android.mazerunner;

import com.bugsnag.android.JsonStream;

import java.io.IOException;

public class CustomSerializedException extends Throwable implements JsonStream.Streamable {

    @Override
    public void toStream(JsonStream writer) throws IOException {
        writer.beginObject();
        writer.name("errorClass").value("Handled Error!");
        writer.name("message").value("all work and no play");
        writer.name("type").value("JS");
        writer.name("specialField").value("layer cake");
        writer.name("stacktrace");
        writer.beginArray();

        // Frame 0
        writer.beginObject();
        writer.name("method").value("foo()");
        writer.name("file").value("src/Giraffe.js");
        writer.name("lineNumber").value(200);
        writer.name("extraNumber").value(43);
        writer.endObject();

        // Frame 1
        writer.beginObject();
        writer.name("method").value("bar()");
        writer.name("file").value("parser.js");
        writer.name("someAddress").value("0xea14616b0");
        writer.endObject();

        // Frame 2
        writer.beginObject();
        writer.name("someAddress").value("0x000000000");
        writer.endObject();

        writer.endArray();
        writer.endObject();
    }
}
