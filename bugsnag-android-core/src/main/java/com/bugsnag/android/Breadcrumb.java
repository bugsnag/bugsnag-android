package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class Breadcrumb implements JsonStream.Streamable {

    @NonNull
    private String name;

    @NonNull
    private BreadcrumbType type;

    @NonNull
    private final String timestamp;

    @NonNull
    private Map<String, Object> metadata;

    // Overloaded Constructor to support custom timestamps
    Breadcrumb(@NonNull String name, @NonNull BreadcrumbType type, @NonNull Map<String, Object> metadata, @Nullable Date customTimestamp) {
        this.name = name;
        this.type = type;
        this.metadata = new HashMap<>(metadata);

        // Use custom timestamp if provided; otherwise fallback to current system clock
        Date date = (customTimestamp != null) ? customTimestamp : new Date();
        this.timestamp = DateUtils.toIso8601(date);
    }

    // Legacy constructor chaining to preserve backward compatibility
    Breadcrumb(@NonNull String name, @NonNull BreadcrumbType type, @NonNull Map<String, Object> metadata) {
        this(name, type, metadata, null);
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public BreadcrumbType getType() {
        return type;
    }

    public void setType(@NonNull BreadcrumbType type) {
        this.type = type;
    }

    @NonNull
    public String getTimestamp() {
        return timestamp;
    }

    @NonNull
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(@NonNull Map<String, Object> metadata) {
        this.metadata = new HashMap<>(metadata);
    }

    @Override
    public void toStream(@NonNull JsonStream stream) throws IOException {
        stream.beginObject();
        stream.name("name").value(name);
        stream.name("timestamp").value(timestamp);
        stream.name("type").value(type.toString());
        stream.name("metaData").value(metadata);
        stream.endObject();
    }
}
