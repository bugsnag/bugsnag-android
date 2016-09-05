package com.bugsnag.android;

/**
 * Used to store information about an exception that was not provided with an exception object
 */
public class BugsnagException extends Throwable {

    /** The name of the exception (used instead of the exception class) */
    private String name;

    /**
     * Constructor
     * @param name The name of the exception (used instead of the exception class)
     * @param message The exception message
     * @param frames The exception stack trace
     */
    public BugsnagException(String name, String message, StackTraceElement[] frames) {
        super(message);

        super.setStackTrace(frames);
        this.name = name;
    }

    /**
     * Constructor
     * @param t Any throwable passed in, used to get the class name to use as the name
     */
    public BugsnagException(Throwable t) {
        super(t.getLocalizedMessage(), t.getCause());

        super.setStackTrace(t.getStackTrace());
        this.name = t.getClass().getName();
    }

    /**
     * @return The name of the exception (used instead of the exception class)
     */
    public String getName() {
        return name;
    }
}
