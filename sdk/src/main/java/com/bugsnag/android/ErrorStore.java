package com.bugsnag.android;

import android.content.Context;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;

/**
 * Store and flush Error reports which couldn't be sent immediately due to
 * lack of network connectivity.
 */
class ErrorStore {

    private static final String UNSENT_ERROR_PATH = "/bugsnag-errors/";
    private static final String STARTUP_CRASH = "_startupcrash";
    private static final int MAX_STORED_ERRORS = 100;

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

    @NonNull
    private final Configuration config;

    @Nullable
    final String path;

    ErrorStore(@NonNull Configuration config, @NonNull Context appContext) {
        this.config = config;

        String path;
        try {
            path = appContext.getCacheDir().getAbsolutePath() + UNSENT_ERROR_PATH;

            File outFile = new File(path);
            outFile.mkdirs();
            if (!outFile.exists()) {
                Logger.warn("Could not prepare error storage directory");
                path = null;
            }
        } catch (Exception e) {
            Logger.warn("Could not prepare error storage directory", e);
            path = null;
        }
        this.path = path;
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
        if (path == null) {
            return;
        }

        try {
            Async.run(new Runnable() {
                @Override
                public void run() {
                    // Look up all saved error files
                    File exceptionDir = new File(path);
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
            Report report = new Report(config.getApiKey(), errorFile);
            errorReportApiClient.postReport(config.getEndpoint(), report);

            Logger.info("Deleting sent error file " + errorFile.getName());
            if (!errorFile.delete()) {
                errorFile.deleteOnExit();
            }
        } catch (DefaultHttpClient.NetworkException e) {
            Logger.warn("Could not send previously saved error(s) to Bugsnag, will try again later", e);
        } catch (Exception e) {
            Logger.warn("Problem sending unsent error from disk", e);
            if (!errorFile.delete()) {
                errorFile.deleteOnExit();
            }
        }
    }

    /**
     * Write an error to disk, for later sending. Returns the filename of the report location
     */
    @Nullable
    String write(@NonNull Error error) {
        if (path == null) {
            return null;
        }

        // Limit number of saved errors to prevent disk space issues
        File exceptionDir = new File(path);
        if (exceptionDir.isDirectory()) {
            File[] files = exceptionDir.listFiles();
            if (files != null && files.length >= MAX_STORED_ERRORS) {
                // Sort files then delete the first one (oldest timestamp)
                Arrays.sort(files, ERROR_REPORT_COMPARATOR);
                Logger.warn(String.format("Discarding oldest error as stored error limit reached (%s)", files[0].getPath()));
                if (!files[0].delete()) {
                    files[0].deleteOnExit();
                }
            }
        }

        MetaData metaData = error.getMetaData();

        boolean isStartupCrash = metaData != null &&
            metaData.getTab(ExceptionHandler.LAUNCH_CRASH_TAB)
                .containsKey(ExceptionHandler.LAUNCH_CRASH_KEY);
        String suffix = isStartupCrash ? STARTUP_CRASH : "";
        String filename = String.format(Locale.US, "%s%d%s.json", path, System.currentTimeMillis(), suffix);
        Writer out = null;
        try {
            out = new FileWriter(filename);

            JsonStream stream = new JsonStream(out);
            stream.value(error);
            stream.close();

            Logger.info(String.format("Saved unsent error to disk (%s) ", filename));
            return filename;
        } catch (Exception e) {
            Logger.warn(String.format("Couldn't save unsent error to disk (%s) ", filename), e);
        } finally {
            IOUtils.closeQuietly(out);
        }
        return null;
    }

    boolean isLaunchCrashReport(File file) {
        String name = file.getName();
        return name.matches("[0-9]+_startupcrash\\.json");
    }

    private List<File> findLaunchCrashReports() {
        if (path == null) {
            return Collections.emptyList();
        }

        File exceptionDir = new File(path);
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

}
