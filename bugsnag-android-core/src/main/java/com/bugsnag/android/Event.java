package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
public class Event implements JsonStream.Streamable, MetadataAware, UserAware {

    final EventImpl impl;
    private final Logger logger;

    Event(@Nullable Throwable originalError,
          @NonNull ImmutableConfig config,
          @NonNull HandledState handledState,
          @NonNull Logger logger) {
        this(originalError, config, handledState, new Metadata(), logger);
    }

    Event(@Nullable Throwable originalError,
          @NonNull ImmutableConfig config,
          @NonNull HandledState handledState,
          @NonNull Metadata metadata,
          @NonNull Logger logger) {
        this(new EventImpl(originalError, config, handledState, metadata), logger);
    }

    Event(@NonNull EventImpl impl, @NonNull Logger logger) {
        this.impl = impl;
        this.logger = logger;
    }

    private void logNull(String property) {
        logger.e("Invalid null value supplied to config." + property + ", ignoring");
    }

    public boolean getUnhandled() {
        return impl.isUnhandled();
    }

    @NonNull
    public List<Error> getErrors() {
        return impl.getErrors();
    }

    @NonNull
    public List<Thread> getThreads() {
        return impl.getThreads();
    }

    @NonNull
    public List<Breadcrumb> getBreadcrumbs() {
        return impl.getBreadcrumbs();
    }

    @NonNull
    public AppWithState getApp() {
        return impl.getApp();
    }

    @NonNull
    public DeviceWithState getDevice() {
        return impl.getDevice();
    }

    public void setApiKey(@NonNull String apiKey) {
        if (apiKey != null) {
            impl.setApiKey(apiKey);
        } else {
            logNull("apiKey");
        }
    }

    @NonNull
    public String getApiKey() {
        return impl.getApiKey();
    }

    public void setSeverity(@NonNull Severity severity) {
        if (severity != null) {
            impl.setSeverity(severity);
        } else {
            logNull("severity");
        }
    }

    @NonNull
    public Severity getSeverity() {
        return impl.getSeverity();
    }


    public void setGroupingHash(@Nullable String groupingHash) {
        impl.setGroupingHash(groupingHash);
    }

    @Nullable
    public String getGroupingHash() {
        return impl.getGroupingHash();
    }

    public void setContext(@Nullable String context) {
        impl.setContext(context);
    }

    @Nullable
    public String getContext() {
        return impl.getContext();
    }

    @Override
    public void setUser(@Nullable String id, @Nullable String email, @Nullable String name) {
        impl.setUser(id, email, name);
    }

    @Override
    @NonNull
    public User getUser() {
        return impl.getUser();
    }

    @Override
    public void addMetadata(@NonNull String section, @NonNull Map<String, ?> value) {
        if (section != null && value != null) {
            impl.addMetadata(section, value);
        } else {
            logNull("addMetadata");
        }
    }

    @Override
    public void addMetadata(@NonNull String section, @NonNull String key, @Nullable Object value) {
        if (section != null && key != null) {
            impl.addMetadata(section, key, value);
        } else {
            logNull("addMetadata");
        }
    }

    @Override
    public void clearMetadata(@NonNull String section) {
        if (section != null) {
            impl.clearMetadata(section);
        } else {
            logNull("clearMetadata");
        }
    }

    @Override
    public void clearMetadata(@NonNull String section, @NonNull String key) {
        if (section != null && key != null) {
            impl.clearMetadata(section, key);
        } else {
            logNull("clearMetadata");
        }
    }

    @Override
    @Nullable
    public Map<String, Object> getMetadata(@NonNull String section) {
        if (section != null) {
            return impl.getMetadata(section);
        } else {
            logNull("getMetadata");
            return null;
        }
    }

    @Override
    @Nullable
    public Object getMetadata(@NonNull String section, @NonNull String key) {
        if (section != null && key != null) {
            return impl.getMetadata(section, key);
        } else {
            logNull("getMetadata");
            return null;
        }
    }

    @Override
    public void toStream(@NonNull JsonStream stream) throws IOException {
        impl.toStream(stream);
    }

    public boolean isUnhandled() {
        return impl.isUnhandled();
    }

    protected boolean shouldDiscardClass() {
        return impl.shouldDiscardClass();
    }

    protected void updateSeverityInternal(@NonNull Severity severity) {
        impl.updateSeverityInternal(severity);
    }

    void setApp(@NonNull AppWithState app) {
        impl.app = app;
    }

    void setDevice(@NonNull DeviceWithState device) {
        impl.device = device;
    }

    void setBreadcrumbs(@NonNull List<Breadcrumb> breadcrumbs) {
        impl.setBreadcrumbs(breadcrumbs);
    }

    @Nullable
    Session getSession() {
        return impl.session;
    }

    void setSession(@Nullable Session session) {
        impl.session = session;
    }
}
