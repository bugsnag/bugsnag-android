package com.bugsnag.android;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * Store and flush Error reports which couldn't be sent immediately due to
 * lack of network connectivity.
 */
class ErrorStore extends FileStore<Error> {

    private static final String STARTUP_CRASH = "_startupcrash";
    private static final long LAUNCH_CRASH_TIMEOUT_MS = 2000;
    private static final int LAUNCH_CRASH_POLL_MS = 50;

    private volatile boolean flushOnLaunchCompleted = false;
    private final Semaphore semaphore = new Semaphore(1);

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

    ErrorStore(@NonNull Configuration config, @NonNull Context appContext) {
        super(config, appContext,
            "/bugsnag-errors/", 128, ERROR_REPORT_COMPARATOR);
    }

    void flushOnLaunch(final ErrorReportApiClient errorReportApiClient) {
        final List<File> crashReports = findLaunchCrashReports();

        if (crashReports.isEmpty() || config.getLaunchCrashThresholdMs() == 0) {
            flushAsync(errorReportApiClient); // if disabled or no startup crash, flush async
        } else {

            // Block the main thread for a 2 second interval as the app may crash very soon.
            // The request itself will run in a background thread and will continue after the 2
            // second period until the request completes, or the app crashes.
            flushOnLaunchCompleted = false;
            Logger.info("Attempting to send launch crash reports");

            Async.run(new Runnable() {
                @Override
                public void run() {
                    flushReports(crashReports, errorReportApiClient);
                    flushOnLaunchCompleted = true;
                }
            });

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
    }

    /**
     * Flush any on-disk errors to Bugsnag
     */
    void flushAsync(final ErrorReportApiClient errorReportApiClient) {
        if (storeDirectory == null) {
            return;
        }

        try {
            Async.run(new Runnable() {
                @Override
                public void run() {
                    flushReports(findStoredFiles(), errorReportApiClient);
                }
            });
        } catch (RejectedExecutionException exception) {
            Logger.warn("Failed to flush all on-disk errors, retaining unsent errors for later.");
        }
    }

    private void flushReports(Collection<File> storedReports,
                              ErrorReportApiClient apiClient) {
        if (!storedReports.isEmpty() && semaphore.tryAcquire(1)) {
            try {
                Logger.info(String.format(Locale.US,
                    "Sending %d saved error(s) to Bugsnag", storedReports.size()));

                for (File errorFile : storedReports) {
                    flushErrorReport(errorFile, apiClient);
                }
            } finally {
                semaphore.release(1);
            }
        }
    }

    private void flushErrorReport(File errorFile, ErrorReportApiClient errorReportApiClient) {
        try {
            Report report = new Report(config.getApiKey(), errorFile);
            errorReportApiClient.postReport(config.getEndpoint(), report,
                config.getErrorApiHeaders());

            deleteStoredFiles(Collections.singleton(errorFile));
            Logger.info("Deleting sent error file " + errorFile.getName());
        } catch (NetworkException exception) {
            cancelQueuedFiles(Collections.singleton(errorFile));
            Logger.warn("Could not send previously saved error(s)"
                + " to Bugsnag, will try again later", exception);
        } catch (Exception exception) {
            deleteStoredFiles(Collections.singleton(errorFile));
            Logger.warn("Problem sending unsent error from disk", exception);
        }
    }

    boolean isLaunchCrashReport(File file) {
        return file.getName().endsWith("_startupcrash.json");
    }

    private List<File> findLaunchCrashReports() {
        Collection<File> storedFiles = findStoredFiles();
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
    String getFilename(Error error) {
        boolean isStartupCrash = isStartupCrash(AppData.getDurationMs());
        String suffix = isStartupCrash ? STARTUP_CRASH : "";
        return String.format(Locale.US, "%s%d_%s%s.json",
            storeDirectory, System.currentTimeMillis(), UUID.randomUUID().toString(), suffix);
    }

    boolean isStartupCrash(long durationMs) {
        return durationMs < config.getLaunchCrashThresholdMs();
    }

}
