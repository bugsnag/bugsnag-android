package com.bugsnag.android;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.WeakHashMap;

/**
 * Provides automatic notification hooks for unhandled exceptions.
 */
class ExceptionHandler implements UncaughtExceptionHandler {

    private  static final String STRICT_MODE_VIOLATION_CLZ_NAME = "android.os.StrictMode$StrictModeViolation";

    private final UncaughtExceptionHandler originalHandler;
    final WeakHashMap<Client, Boolean> clientMap = new WeakHashMap<Client, Boolean>();

    static void enable(Client client) {
        UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();

        // Find or create the Bugsnag ExceptionHandler
        ExceptionHandler bugsnagHandler;
        if (currentHandler instanceof ExceptionHandler) {
            bugsnagHandler = (ExceptionHandler) currentHandler;
        } else {
            bugsnagHandler = new ExceptionHandler(currentHandler);
            Thread.setDefaultUncaughtExceptionHandler(bugsnagHandler);
        }

        // Subscribe this client to uncaught exceptions
        bugsnagHandler.clientMap.put(client, true);
    }

    static void disable(Client client) {
        // Find the Bugsnag ExceptionHandler
        UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (currentHandler instanceof ExceptionHandler) {
            // Unsubscribe this client from uncaught exceptions
            ExceptionHandler bugsnagHandler = (ExceptionHandler) currentHandler;
            bugsnagHandler.clientMap.remove(client);

            // Remove the Bugsnag ExceptionHandler if no clients are subscribed
            if (bugsnagHandler.clientMap.size() == 0) {
                Thread.setDefaultUncaughtExceptionHandler(bugsnagHandler.originalHandler);
            }
        }
    }

    public ExceptionHandler(UncaughtExceptionHandler originalHandler) {
        this.originalHandler = originalHandler;
    }

    public void uncaughtException(Thread t, Throwable e) {
        isStrictModeThrowable(e);

        // Notify any subscribed clients of the uncaught exception
        for (Client client : clientMap.keySet()) {
            client.cacheAndNotify(e, Severity.ERROR);
        }

        // Pass exception on to original exception handler
        if (originalHandler != null) {
            originalHandler.uncaughtException(t, e);
        } else {
            System.err.printf("Exception in thread \"%s\" ", t.getName());
            e.printStackTrace(System.err);
        }
    }

    boolean isStrictModeThrowable(Throwable e) {
        Throwable cause = getRootCause(e);
        Class<? extends Throwable> causeClass = cause.getClass();
        String simpleName = causeClass.getName();
        return STRICT_MODE_VIOLATION_CLZ_NAME.equals(simpleName);
    }

    /**
     * Recurse to get the original cause of the throwable
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
