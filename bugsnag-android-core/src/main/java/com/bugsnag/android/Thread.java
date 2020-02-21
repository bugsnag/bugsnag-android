package com.bugsnag.android;

import androidx.annotation.NonNull;

import java.io.IOException;

/**
 * A representation of a thread recorded in an {@link Event}
 */
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

    /**
     * Sets the unique ID of the thread (from {@link java.lang.Thread})
     */
    public void setId(long id) {
        impl.setId(id);
    }

    /**
     * Gets the unique ID of the thread (from {@link java.lang.Thread})
     */
    public long getId() {
        return impl.getId();
    }

    /**
     * Sets the name of the thread (from {@link java.lang.Thread})
     */
    public void setName(@NonNull String name) {
        if (name != null) {
            impl.setName(name);
        } else {
            error("name");
        }
    }

    /**
     * Gets the name of the thread (from {@link java.lang.Thread})
     */
    @NonNull
    public String getName() {
        return impl.getName();
    }

    /**
     * Sets the type of thread based on the originating platform (intended for internal use only)
     */
    public void setType(@NonNull ThreadType type) {
        if (type != null) {
            impl.setType(type);
        } else {
            error("type");
        }
    }

    /**
     * Gets the type of thread based on the originating platform (intended for internal use only)
     */
    @NonNull
    public ThreadType getType() {
        return impl.getType();
    }

    /**
     * Sets whether the thread was the thread that caused the event
     */
    public void setErrorReportingThread(boolean errorReportingThread) {
        impl.setErrorReportingThread(errorReportingThread);
    }

    /**
     * Gets whether the thread was the thread that caused the event
     */
    public boolean getErrorReportingThread() {
        return impl.isErrorReportingThread();
    }

    @Override
    public void toStream(@NonNull JsonStream stream) throws IOException {
        impl.toStream(stream);
    }
}
