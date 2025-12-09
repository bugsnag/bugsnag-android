package com.bugsnag.android;

import androidx.annotation.IntRange;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * A Response represents an HTTP response that forms part of an {@link Event}.
 */
public final class Response extends AbstractHttpEntity implements JsonStream.Streamable {
    private int statusCode;

    /**
     * Constructs a new Response with the specified HTTP status code.
     *
     * @param statusCode the HTTP status code
     */
    public Response(@IntRange(from = 0) int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(@IntRange(from = 0) int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public void toStream(@NotNull JsonStream writer) throws IOException {
        writer.beginObject();
        writer.name("statusCode").value(statusCode);

        writer.name("body").value(getBody());

        long bodyLength = getBodyLength();
        if (bodyLength >= 0) {
            writer.name("bodyLength").value(bodyLength);
        }

        writer.name("headers").value(headers, true);
        writer.endObject();
    }
}
