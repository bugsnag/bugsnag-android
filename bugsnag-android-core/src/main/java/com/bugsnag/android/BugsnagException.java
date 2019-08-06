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
    private String name;
    private String message;

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
        setStackTrace(frames);
        this.name = name;
    }

    BugsnagException(@NonNull Throwable exc) {
        super(exc.getMessage());

        if (exc instanceof BugsnagException) {
            this.message = ((BugsnagException) exc).getMessage();
            this.name = ((BugsnagException) exc).getName();
            this.type = ((BugsnagException) exc).getType();
        } else {
            this.name = exc.getClass().getName();
        }
        setStackTrace(exc.getStackTrace());
        initCause(exc.getCause());
    }

    /**
     * @return The name of the exception (used instead of the exception class)
     */
    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getMessage() {
        return message != null ? message : super.getMessage();
    }

    public void setMessage(@NonNull String message) {
        this.message = message;
    }

    @NonNull
    String getType() {
        return type;
    }

    void setType(@NonNull String type) {
        this.type = type;
    }
}
