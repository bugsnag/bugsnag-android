package com.bugsnag.android;

import android.content.Context;
import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * Store and flush Error reports which couldn't be sent immediately due to
 * lack of network connectivity.
 */
@ThreadSafe
class ErrorStore extends FileStore<Error> {

    interface Delegate {

        /**
         * Invoked when a cached error report cannot be read.
         *
         * @param exception the error encountered reading/delivering the file
         * @param errorFile file which could not be read
         */
        void onErrorReadFailure(Exception exception, File errorFile);
    }

    private static final String HEADER_API_PAYLOAD_VERSION = "Bugsnag-Payload-Version";
    private static final String HEADER_API_KEY = "Bugsnag-Api-Key";
    private static final String HEADER_BUGSNAG_SENT_AT = "Bugsnag-Sent-At";

    private static final String STARTUP_CRASH = "_startupcrash";
    private static final long LAUNCH_CRASH_TIMEOUT_MS = 2000;
    private static final int LAUNCH_CRASH_POLL_MS = 50;

    volatile boolean flushOnLaunchCompleted = false;
    private final Semaphore semaphore = new Semaphore(1);
    private final ImmutableConfig config;
    private final Configuration clientState;
    private final Delegate delegate;

    static final Comparator<File> ERROR_REPORT_COMPARATOR = new Comparator<File>() {
        @Override
        public int compare(File lhs, File rhs) {
            if (lhs == null && rhs == null) {
                return 0;
            }
            if (lhs == null) {
                return 1;
            }
            if (rhs == null) {
                return -1;
            }
            String lhsName = lhs.getName().replaceAll(STARTUP_CRASH, "");
            String rhsName = rhs.getName().replaceAll(STARTUP_CRASH, "");
            return lhsName.compareTo(rhsName);
        }
    };

    ErrorStore(@NonNull ImmutableConfig config, @NonNull Configuration clientState,
               @NonNull Context appContext, Delegate delegate) {
        super(appContext, "/bugsnag-errors/", 128, ERROR_REPORT_COMPARATOR);
        this.config = config;
        this.clientState = clientState;
        this.delegate = delegate;
    }

    void flushOnLaunch() {
        if (config.getLaunchCrashThresholdMs() != 0) {
            List<File> storedFiles = findStoredFiles();
            final List<File> crashReports = findLaunchCrashReports(storedFiles);

            if (!crashReports.isEmpty()) {

                // Block the main thread for a 2 second interval as the app may crash very soon.
                // The request itself will run in a background thread and will continue after the 2
                // second period until the request completes, or the app crashes.
                flushOnLaunchCompleted = false;
                Logger.info("Attempting to send launch crash reports");

                try {
                    Async.run(new Runnable() {
                        @Override
                        public void run() {
                            flushReports(crashReports);
                            flushOnLaunchCompleted = true;
                        }
                    });
                } catch (RejectedExecutionException ex) {
                    Logger.warn("Failed to flush launch crash reports", ex);
                    flushOnLaunchCompleted = true;
                }

                long waitMs = 0;

                while (!flushOnLaunchCompleted && waitMs < LAUNCH_CRASH_TIMEOUT_MS) {
                    try {
                        Thread.sleep(LAUNCH_CRASH_POLL_MS);
                        waitMs += LAUNCH_CRASH_POLL_MS;
                    } catch (InterruptedException exception) {
                        Logger.warn("Interrupted while waiting for launch crash report request");
                    }
                }
                Logger.info("Continuing with Bugsnag initialisation");
            }
            cancelQueuedFiles(storedFiles); // cancel all previously found files
        }

        flushAsync(); // flush any remaining errors async that weren't delivered
    }

    /**
     * Flush any on-disk errors to Bugsnag
     */
    void flushAsync() {
        if (storeDirectory == null) {
            return;
        }

        try {
            Async.run(new Runnable() {
                @Override
                public void run() {
                    flushReports(findStoredFiles());
                }
            });
        } catch (RejectedExecutionException exception) {
            Logger.warn("Failed to flush all on-disk errors, retaining unsent errors for later.");
        }
    }

    void flushReports(Collection<File> storedReports) {
        if (!storedReports.isEmpty() && semaphore.tryAcquire(1)) {
            try {
                Logger.info(String.format(Locale.US,
                    "Sending %d saved error(s) to Bugsnag", storedReports.size()));

                for (File errorFile : storedReports) {
                    flushErrorReport(errorFile);
                }
            } finally {
                semaphore.release(1);
            }
        }
    }

    private void flushErrorReport(File errorFile) {
        try {
            Report report;

            if (clientState.getBeforeSendTasks().isEmpty()) {
                report = new Report(config.getApiKey(), errorFile);
            } else {
                Error error = ErrorReader.readError(config, clientState, errorFile);
                report = new Report(config.getApiKey(), error);

                for (BeforeSend beforeSend : clientState.getBeforeSendTasks()) {
                    try {
                        if (!beforeSend.run(report)) {
                            deleteStoredFiles(Collections.singleton(errorFile));
                            Logger.info("Deleting cancelled error file " + errorFile.getName());
                            return;
                        }
                    } catch (Throwable ex) {
                        Logger.warn("BeforeSend threw an Exception", ex);
                    }
                }
            }

            DeliveryParams deliveryParams = config.errorApiDeliveryParams();
            DeliveryStatus deliveryStatus = config.getDelivery().deliver(report, deliveryParams);

            switch (deliveryStatus) {
                case DELIVERED:
                    deleteStoredFiles(Collections.singleton(errorFile));
                    Logger.info("Deleting sent error file " + errorFile.getName());
                    break;
                case UNDELIVERED:
                    cancelQueuedFiles(Collections.singleton(errorFile));
                    Logger.warn("Could not send previously saved error(s)"
                            + " to Bugsnag, will try again later");
                    break;
                case FAILURE:
                    Exception exc = new RuntimeException("Failed to deliver report");
                    handleErrorFlushFailure(exc, errorFile);
                    break;
                default:
                    break;
            }
        } catch (Exception exception) {
            handleErrorFlushFailure(exception, errorFile);
        }
    }

    private void handleErrorFlushFailure(Exception exc, File errorFile) {
        if (delegate != null) {
            delegate.onErrorReadFailure(exc, errorFile);
        }
        deleteStoredFiles(Collections.singleton(errorFile));
    }

    boolean isLaunchCrashReport(File file) {
        return file.getName().endsWith("_startupcrash.json");
    }

    private List<File> findLaunchCrashReports(Collection<File> storedFiles) {
        List<File> launchCrashes = new ArrayList<>();

        for (File file : storedFiles) {
            if (isLaunchCrashReport(file)) {
                launchCrashes.add(file);
            }
        }
        return launchCrashes;
    }

    @NonNull
    @Override
    String getFilename(Object object) {
        String suffix = "";

        if (object instanceof Error) {
            Error error = (Error) object;

            Map<String, Object> appData = error.getAppData();
            if (appData instanceof Map) {
                Object duration = appData.get("duration");
                if (duration instanceof Number
                    && isStartupCrash(((Number) appData.get("duration")).longValue())) {
                    suffix = STARTUP_CRASH;
                }
            }
        } else {
            suffix = "not-jvm";
        }
        String uuid = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        return String.format(Locale.US, "%s%d_%s%s.json",
            storeDirectory, timestamp, uuid, suffix);
    }

    boolean isStartupCrash(long durationMs) {
        return durationMs < config.getLaunchCrashThresholdMs();
    }
}
