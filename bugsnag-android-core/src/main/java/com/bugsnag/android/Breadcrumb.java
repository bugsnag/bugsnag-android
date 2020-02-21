package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
public class Breadcrumb implements JsonStream.Streamable {

    private final BreadcrumbImpl impl;
    private final Logger logger;

    Breadcrumb(@NonNull String message, @NonNull Logger logger) {
        this.impl = new BreadcrumbImpl(message);
        this.logger = logger;
    }

    Breadcrumb(@NonNull String message,
               @NonNull BreadcrumbType type,
               @Nullable Map<String, Object> metadata,
               @NonNull Date timestamp,
               @NonNull Logger logger) {
        this.impl = new BreadcrumbImpl(message, type, metadata, timestamp);
        this.logger = logger;
    }

    private void error(String property) {
        logger.e("Invalid null value supplied to breadcrumb." + property + ", ignoring");
    }

    public void setMessage(@NonNull String message) {
        if (message != null) {
            impl.setMessage(message);
        } else {
            error("message");
        }
    }

    @NonNull
    public String getMessage() {
        return impl.getMessage();
    }

    public void setType(@NonNull BreadcrumbType type) {
        if (type != null) {
            impl.setType(type);
        } else {
            error("type");
        }
    }

    @NonNull
    public BreadcrumbType getType() {
        return impl.getType();
    }

    public void setMetadata(@NonNull Map<String, Object> metadata) {
        impl.setMetadata(metadata);
    }

    @NonNull
    public Map<String, Object> getMetadata() {
        return impl.getMetadata();
    }

    @NonNull
    public Date getTimestamp() {
        return impl.getTimestamp();
    }

    @Override
    public void toStream(@NonNull JsonStream stream) throws IOException {
        impl.toStream(stream);
    }
}
