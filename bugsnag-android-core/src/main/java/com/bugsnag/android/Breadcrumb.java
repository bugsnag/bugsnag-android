package com.bugsnag.android;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Breadcrumb implements JsonStream.Streamable {

    private static final String DEFAULT_NAME = "manual";
    private static final String MESSAGE_METAKEY = "message";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String NAME_KEY = "message";
    private static final String METADATA_KEY = "metaData";
    private static final String TYPE_KEY = "type";

    @NonNull
    private final String timestamp;

    @NonNull
    private final String message;

    @NonNull
    private final BreadcrumbType type;

    @NonNull
    private final Map<String, String> metadata;

    Breadcrumb(@NonNull String message) {
        this(DEFAULT_NAME, BreadcrumbType.MANUAL,
                Collections.singletonMap(MESSAGE_METAKEY, message));
    }

    Breadcrumb(@NonNull String message,
               @NonNull BreadcrumbType type,
               @NonNull Map<String, String> metadata) {
        this(message, type, new Date(), metadata);
    }

    Breadcrumb(@NonNull String message,
               @NonNull BreadcrumbType type,
               @NonNull Date captureDate,
               @NonNull Map<String, String> metadata) {
        this.timestamp = DateUtils.toIso8601(captureDate);
        this.type = type;
        this.message = message;
        this.metadata = new HashMap<>(metadata);
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    @NonNull
    public BreadcrumbType getType() {
        return type;
    }

    @NonNull
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @NonNull
    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        writer.name(TIMESTAMP_KEY).value(this.timestamp);
        writer.name(NAME_KEY).value(this.message);
        writer.name(TYPE_KEY).value(this.type.toString());
        writer.name(METADATA_KEY);
        writer.beginObject();

        // sort metadata alphabetically
        List<String> keys = new ArrayList<>(metadata.keySet());
        Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);

        for (String key : keys) {
            writer.name(key).value(metadata.get(key));
        }

        writer.endObject();
        writer.endObject();
    }

    int payloadSize() throws IOException {
        StringWriter writer = new StringWriter();
        JsonStream jsonStream = new JsonStream(writer);
        toStream(jsonStream);

        return writer.toString().length();
    }
}
