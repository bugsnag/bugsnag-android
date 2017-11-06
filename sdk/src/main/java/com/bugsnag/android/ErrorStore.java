package com.bugsnag.android;

import android.content.Context;
import android.os.StrictMode;
import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;

/**
 * Store and flush Error reports which couldn't be sent immediately due to
 * lack of network connectivity.
 */
class ErrorStore extends FileStore<Error> {

    private static final String STARTUP_CRASH = "_startupcrash";

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
        super(config, appContext, "/bugsnag-errors/", 128, ERROR_REPORT_COMPARATOR);
    }

    void flushOnLaunch(ErrorReportApiClient errorReportApiClient) {
        List<File> crashReports = findLaunchCrashReports();

        if (crashReports.isEmpty() && config.getLaunchCrashThresholdMs() > 0) {
            flushAsync(errorReportApiClient); // if disabled or no startup crash, flush async
        } else {
            // flush synchronously as the app may crash very soon.
            // need to disable strictmode and this also risks ANR,
            // but can capture reports which may not otherwise be sent
            StrictMode.ThreadPolicy originalThreadPolicy = StrictMode.getThreadPolicy();
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);

            for (File crashReport : crashReports) {
                flushErrorReport(crashReport, errorReportApiClient);
            }
            StrictMode.setThreadPolicy(originalThreadPolicy);
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
                    // Look up all saved error files
                    File exceptionDir = new File(storeDirectory);
                    if (!exceptionDir.exists() || !exceptionDir.isDirectory()) return;

                    File[] errorFiles = exceptionDir.listFiles();
                    if (errorFiles != null && errorFiles.length > 0) {
                        Logger.info(String.format(Locale.US, "Sending %d saved error(s) to Bugsnag", errorFiles.length));

                        for (File errorFile : errorFiles) {
                            flushErrorReport(errorFile, errorReportApiClient);
                        }
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            Logger.warn("Failed to flush all on-disk errors, retaining unsent errors for later.");
        }
    }

    private void flushErrorReport(File errorFile, ErrorReportApiClient errorReportApiClient) {
        try {
            Report report = new Report(errorFile);
            errorReportApiClient.postReport(config.getEndpoint(), report, config.getErrorApiHeaders());

            Logger.info("Deleting sent error file " + errorFile.getName());
            if (!errorFile.delete()) {
                errorFile.deleteOnExit();
            }
        } catch (NetworkException e) {
            Logger.warn("Could not send previously saved error(s) to Bugsnag, will try again later", e);
        } catch (Exception e) {
            Logger.warn("Problem sending unsent error from disk", e);
            if (!errorFile.delete()) {
                errorFile.deleteOnExit();
            }
        }
    }

    boolean isLaunchCrashReport(File file) {
        String name = file.getName();
        return name.matches("[0-9]+_startupcrash\\.json");
    }

    private List<File> findLaunchCrashReports() {
        if (storeDirectory == null) {
            return Collections.emptyList();
        }

        File exceptionDir = new File(storeDirectory);
        List<File> launchCrashes = new ArrayList<>();

        if (exceptionDir.isDirectory()) {
            File[] files = exceptionDir.listFiles();

            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (isLaunchCrashReport(file)) {
                        launchCrashes.add(file);
                    }
                }
            }
        }
        return launchCrashes;
    }

    @NonNull
    @Override
    String getFilename(Error error) {
        MetaData metaData = error.getMetaData();

        boolean isStartupCrash = metaData != null &&
            metaData.getTab(ExceptionHandler.LAUNCH_CRASH_TAB)
                .containsKey(ExceptionHandler.LAUNCH_CRASH_KEY);
        String suffix = isStartupCrash ? STARTUP_CRASH : "";
        return String.format(Locale.US, "%s%d%s.json", storeDirectory, System.currentTimeMillis(), suffix);
    }

}
