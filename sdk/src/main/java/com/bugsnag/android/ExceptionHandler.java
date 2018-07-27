package com.bugsnag.android;

import android.os.StrictMode;
import android.support.annotation.NonNull;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Provides automatic notification hooks for unhandled exceptions.
 */
class ExceptionHandler implements UncaughtExceptionHandler {

    private static final String STRICT_MODE_TAB = "StrictMode";
    private static final String STRICT_MODE_KEY = "Violation";

    private final UncaughtExceptionHandler originalHandler;
    private final StrictModeHandler strictModeHandler = new StrictModeHandler();
    final Map<Client, Boolean> clientMap = new WeakHashMap<>();

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
            if (bugsnagHandler.clientMap.isEmpty()) {
                Thread.setDefaultUncaughtExceptionHandler(bugsnagHandler.originalHandler);
            }
        }
    }

    ExceptionHandler(UncaughtExceptionHandler originalHandler) {
        this.originalHandler = originalHandler;
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        boolean strictModeThrowable = strictModeHandler.isStrictModeThrowable(throwable);

        // Notify any subscribed clients of the uncaught exception
        for (Client client : clientMap.keySet()) {
            MetaData metaData = new MetaData();
            String violationDesc = null;

            if (strictModeThrowable) { // add strictmode policy violation to metadata
                violationDesc = strictModeHandler.getViolationDescription(throwable.getMessage());
                metaData = new MetaData();
                metaData.addToTab(STRICT_MODE_TAB, STRICT_MODE_KEY, violationDesc);
            }

            String severityReason = strictModeThrowable
                ? HandledState.REASON_STRICT_MODE : HandledState.REASON_UNHANDLED_EXCEPTION;

            if (strictModeThrowable) { // writes to disk on main thread
                StrictMode.ThreadPolicy originalThreadPolicy = StrictMode.getThreadPolicy();
                StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);

                client.cacheAndNotify(throwable, Severity.ERROR,
                    metaData, severityReason, violationDesc);

                StrictMode.setThreadPolicy(originalThreadPolicy);
            } else {
                client.cacheAndNotify(throwable, Severity.ERROR,
                    metaData, severityReason, violationDesc);
            }
        }

        // Pass exception on to original exception handler
        if (originalHandler != null) {
            originalHandler.uncaughtException(thread, throwable);
        } else {
            System.err.printf("Exception in thread \"%s\" ", thread.getName());
            Logger.warn("Exception", throwable);
        }
    }
}
