package com.bugsnag.android;

import android.support.annotation.NonNull;

/**
 * Used to store information about an exception that was not provided with an exception object
 */
@ThreadSafe
public class BugsnagException extends Throwable {

    private static final long serialVersionUID = 5068182621179433346L;
    /**
     * The name of the exception (used instead of the exception class)
     */
    private final String name;

    private String type;

    /**
     * Constructor
     *
     * @param name    The name of the exception (used instead of the exception class)
     * @param message The exception message
     * @param frames  The exception stack trace
     */
    public BugsnagException(@NonNull String name,
                            @NonNull String message,
                            @NonNull StackTraceElement[] frames) {
        super(message);

        super.setStackTrace(frames);
        this.name = name;
    }

    /**
     * @return The name of the exception (used instead of the exception class)
     */
    @NonNull
    public String getName() {
        return name;
    }

    String getType() {
        return type;
    }

    void setType(String type) {
        this.type = type;
    }
}
