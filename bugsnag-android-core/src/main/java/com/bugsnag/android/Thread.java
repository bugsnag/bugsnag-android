package com.bugsnag.android;

import androidx.annotation.NonNull;

import java.io.IOException;

@SuppressWarnings("ConstantConditions")
public class Thread implements JsonStream.Streamable {

    private final ThreadImpl impl;
    private final Logger logger;

    Thread(
            long id,
            @NonNull String name,
            @NonNull ThreadType type,
            boolean errorReportingThread,
            @NonNull Stacktrace stacktrace,
            @NonNull Logger logger) {
        this.impl = new ThreadImpl(id, name, type, errorReportingThread, stacktrace);
        this.logger = logger;
    }

    private void error(String property) {
        logger.e("Invalid null value supplied to thread." + property + ", ignoring");
    }

    public void setId(long id) {
        impl.setId(id);
    }

    public long getId() {
        return impl.getId();
    }

    public void setName(@NonNull String name) {
        if (name != null) {
            impl.setName(name);
        } else {
            error("name");
        }
    }

    @NonNull
    public String getName() {
        return impl.getName();
    }

    public void setType(@NonNull ThreadType type) {
        if (type != null) {
            impl.setType(type);
        } else {
            error("type");
        }
    }

    @NonNull
    public ThreadType getType() {
        return impl.getType();
    }

    public void setErrorReportingThread(boolean errorReportingThread) {
        impl.setErrorReportingThread(errorReportingThread);
    }

    public boolean getErrorReportingThread() {
        return impl.isErrorReportingThread();
    }

    @Override
    public void toStream(@NonNull JsonStream stream) throws IOException {
        impl.toStream(stream);
    }
}
