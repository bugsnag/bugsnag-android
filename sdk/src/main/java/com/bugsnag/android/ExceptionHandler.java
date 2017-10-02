package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Provides automatic notification hooks for unhandled exceptions.
 */
class ExceptionHandler implements UncaughtExceptionHandler {

    private static final String STRICT_MODE_TAB = "StrictMode";
    private static final String STRICT_MODE_KEY = "Violation";
    static final String LAUNCH_CRASH_TAB = "CrashOnLaunch";
    static final String LAUNCH_CRASH_KEY = "Duration (ms)";

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
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        boolean strictModeThrowable = strictModeHandler.isStrictModeThrowable(e);

        // Notify any subscribed clients of the uncaught exception
        Date now = new Date();

        for (Client client : clientMap.keySet()) {
            MetaData metaData = new MetaData();
            String violationDesc = null;

            if (strictModeThrowable) { // add strictmode policy violation to metadata
                violationDesc = strictModeHandler.getViolationDescription(e.getMessage());
                metaData = new MetaData();
                metaData.addToTab(STRICT_MODE_TAB, STRICT_MODE_KEY, violationDesc);
            }

            if (isCrashOnLaunch(client, now)) {
                metaData.addToTab(LAUNCH_CRASH_TAB, LAUNCH_CRASH_KEY, getMsSinceLaunch(client, now));
            }
            
            String severityReason = strictModeThrowable
                ? HandledState.REASON_STRICT_MODE : HandledState.REASON_UNHANDLED_EXCEPTION;
            client.cacheAndNotify(e, Severity.ERROR, metaData, severityReason, violationDesc);
        }

        // Pass exception on to original exception handler
        if (originalHandler != null) {
            originalHandler.uncaughtException(t, e);
        } else {
            System.err.printf("Exception in thread \"%s\" ", t.getName());
            e.printStackTrace(System.err);
        }
    }

    boolean isCrashOnLaunch(Client client, Date now) {

        long delta = getMsSinceLaunch(client, now);
        long thresholdMs = client.config.getLaunchCrashThresholdMs();
        return thresholdMs > 0 && delta <= thresholdMs;
    }

    private long getMsSinceLaunch(Client client, Date now) {
        long launchTimeMs = client.getLaunchTimeMs();
        return now.getTime() - launchTimeMs;
    }

}
