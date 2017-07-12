package com.bugsnag.android;

class StrictModeHandler {

    private static final String STRICT_MODE_CLZ_NAME = "android.os.StrictMode";

    /**
     * Checks whether a throwable was originally thrown from the StrictMode class
     * @param e the throwable
     * @return true if the throwable's root cause is a StrictMode policy violation
     */
    boolean isStrictModeThrowable(Throwable e) {
        Throwable cause = getRootCause(e);
        Class<? extends Throwable> causeClass = cause.getClass();
        String simpleName = causeClass.getName();
        return simpleName.startsWith(STRICT_MODE_CLZ_NAME);
    }

    /**
     * Recurse the stack to get the original cause of the throwable
     *
     * @param t the throwable
     * @return the root cause of the throwable
     */
    private Throwable getRootCause(Throwable t) {
        Throwable cause = t.getCause();

        if (cause == null) {
            return t;
        } else {
            return getRootCause(cause);
        }
    }
}
