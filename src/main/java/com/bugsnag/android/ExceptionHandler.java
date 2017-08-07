package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;
import java.util.WeakHashMap;

/**
 * Provides automatic notification hooks for unhandled exceptions.
 */
class ExceptionHandler implements UncaughtExceptionHandler {

    private static final String KEY_STRICT_MODE_VIOLATION = "StrictModeViolation";
    private final UncaughtExceptionHandler originalHandler;
    private final StrictModeHandler strictModeHandler = new StrictModeHandler();
    final WeakHashMap<Client, Boolean> clientMap = new WeakHashMap<Client, Boolean>();

    static void enable(@NonNull Client client) {
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

    static void disable(@NonNull Client client) {
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

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        boolean strictModeThrowable = strictModeHandler.isStrictModeThrowable(e);

        // Notify any subscribed clients of the uncaught exception
        for (Client client : clientMap.keySet()) {

            if (strictModeThrowable) { // add strictmode policy violation to metadata
                String violationDesc = strictModeHandler.getViolationDescription(e.getMessage());
                MetaData metaData = new MetaData(
                    Collections.<String, Object>singletonMap(KEY_STRICT_MODE_VIOLATION, violationDesc));
                client.cacheAndNotify(e, Severity.ERROR, metaData);
            }
            else {
                client.cacheAndNotify(e, Severity.ERROR, null);
            }
        }

        // Pass exception on to original exception handler
        if (originalHandler != null) {
            originalHandler.uncaughtException(t, e);
        } else {
            System.err.printf("Exception in thread \"%s\" ", t.getName());
            e.printStackTrace(System.err);
        }
    }

}
