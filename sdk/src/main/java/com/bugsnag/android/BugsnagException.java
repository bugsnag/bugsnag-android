package com.bugsnag.android;

import com.facebook.infer.annotation.ThreadSafe;

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

    /**
     * Constructor
     *
     * @param name    The name of the exception (used instead of the exception class)
     * @param message The exception message
     * @param frames  The exception stack trace
     */
    public BugsnagException(String name, String message, StackTraceElement[] frames) {
        super(message);

        super.setStackTrace(frames);
        this.name = name;
    }

    /**
     * @return The name of the exception (used instead of the exception class)
     */
    public String getName() {
        return name;
    }
}
