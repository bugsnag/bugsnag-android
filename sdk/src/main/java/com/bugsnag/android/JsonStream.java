package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class JsonStream extends JsonWriter {
    public interface Streamable {
        void toStream(@NonNull JsonStream stream) throws IOException;
    }

    private final Writer out;

    public JsonStream(Writer out) {
        super(out);
        setSerializeNulls(false);
        this.out = out;
    }

    // Allow chaining name().value()
    @NonNull
    public JsonStream name(@Nullable String name) throws IOException {
        super.name(name);
        return this;
    }

    /**
     * This gives the Streamable the JsonStream instance and
     * allows lets it write itself into the stream.
     */
    public void value(@Nullable Streamable streamable) throws IOException {
        if (streamable == null) {
            nullValue();
            return;
        }
        streamable.toStream(this);
    }

    /**
     * Writes a File (its content) into the stream
     */
    public void value(@NonNull File file) throws IOException {
        super.flush();
        beforeValue(false); // add comma if in array

        // Copy the file contents onto the stream
        FileReader input = null;
        try {
            input = new FileReader(file);
            IOUtils.copy(input, out);
        } finally {
            IOUtils.closeQuietly(input);
        }

        out.flush();
    }

    /**
     * Validates whether an object contains valid JSON, by writing it to a Stream then parsing
     * the contents as JSON
     *
     * @param streamable the streamable to test
     * @return whether the streamable is valid JSON or not
     */
    static boolean isValidJson(Streamable streamable) {
        StringWriter stringWriter = new StringWriter();

        try {
            streamable.toStream(new JsonStream(stringWriter));
            new JSONObject(stringWriter.toString());
            return true;
        } catch (IOException | JSONException e) {
            return false;
        } finally {
            IOUtils.closeQuietly(stringWriter);
        }
    }

}
