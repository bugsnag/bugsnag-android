package com.bugsnag.android;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("ConstantValue")
public abstract class AbstractHttpEntity {
    protected final Map<String, String> headers = new LinkedHashMap<>();

    @Nullable
    private String body;
    private long bodyLength = -1L;

    // package-protected constructor
    AbstractHttpEntity() {
    }

    /**
     * Add a header to this reported HTTP entity.
     *
     * @param name  the name of the header
     * @param value the value of the header
     */
    public void addHeader(@NonNull String name, @NonNull String value) {
        if (name == null || value == null) {
            return;
        }

        headers.put(name, value);
    }

    /**
     * Remove the specified header by name (case-sensitive).
     *
     * @param name the header to remove
     */
    public void removeHeader(@NonNull String name) {
        headers.remove(name);
    }

    /**
     * Return the headers that are set for this HTTP entity.
     *
     * @return the header names
     */
    @NonNull
    public Set<String> getHeaderNames() {
        return Collections.unmodifiableSet(headers.keySet());
    }

    /**
     * Return the HTTP header by name or an empty string if the header is not present.
     *
     * @param headerName the header name (case-sensitive)
     * @return the value of the header or an empty string
     */
    @NonNull
    public String getHeader(@NonNull String headerName) {
        if (headerName == null) {
            return "";
        }

        String headerValue = headers.get(headerName);
        return headerValue != null ? headerValue : "";
    }

    /**
     * Return the captured HTTP body if one has been set, or null.
     *
     * @return the captured HTTP body
     */
    @Nullable
    public String getBody() {
        return body;
    }

    /**
     * Set or clear the captured body.
     *
     * @param body the body to report
     */
    public void setBody(@Nullable String body) {
        this.body = body;
    }

    /**
     * Return the body length (as set with {@link #setBodyLength(long)}) or -1 if none has been set.
     *
     * @return the number of bytes in the body
     */
    public long getBodyLength() {
        return bodyLength;
    }

    /**
     * Change the reported size of the request body size (in bytes).
     *
     * @param bodyLength the number of bytes in the request body
     */
    public void setBodyLength(@IntRange(from = 0L) long bodyLength) {
        if (bodyLength >= 0) {
            this.bodyLength = bodyLength;
        }
    }
}
