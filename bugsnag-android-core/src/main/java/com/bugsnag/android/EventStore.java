package com.bugsnag.android;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * Store and flush Event reports which couldn't be sent immediately due to
 * lack of network connectivity.
 */
class EventStore extends FileStore {

    private static final long LAUNCH_CRASH_TIMEOUT_MS = 2000;
    private static final int LAUNCH_CRASH_POLL_MS = 50;
    private static final int MAX_EVENT_COUNT = 32;

    volatile boolean flushOnLaunchCompleted = false;
    private final Semaphore semaphore = new Semaphore(1);
    private final ImmutableConfig config;
    private final Logger logger;
    private final Delegate delegate;
    private final Notifier notifier;

    static final Comparator<File> EVENT_COMPARATOR = new Comparator<File>() {
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
            return lhs.compareTo(rhs);
        }
    };

    EventStore(@NonNull ImmutableConfig config,
               @NonNull Context appContext, @NonNull Logger logger,
               Notifier notifier, Delegate delegate) {
        super(appContext, "/bugsnag-errors/", MAX_EVENT_COUNT, EVENT_COMPARATOR, logger, delegate);
        this.config = config;
        this.logger = logger;
        this.delegate = delegate;
        this.notifier = notifier;
    }

    void flushOnLaunch() {
        if (config.getLaunchCrashThresholdMs() != 0) {
            List<File> storedFiles = findStoredFiles();
            final List<File> crashReports = findLaunchCrashReports(storedFiles);

            // cancel non-launch crash reports
            storedFiles.removeAll(crashReports);
            cancelQueuedFiles(storedFiles);

            if (!crashReports.isEmpty()) {

                // Block the main thread for a 2 second interval as the app may crash very soon.
                // The request itself will run in a background thread and will continue after the 2
                // second period until the request completes, or the app crashes.
                flushOnLaunchCompleted = false;
                logger.i("Attempting to send launch crash reports");

                try {
                    Async.run(new Runnable() {
                        @Override
                        public void run() {
                            flushReports(crashReports);
                            flushOnLaunchCompleted = true;
                        }
                    });
                } catch (RejectedExecutionException ex) {
                    logger.w("Failed to flush launch crash reports", ex);
                    flushOnLaunchCompleted = true;
                }

                long waitMs = 0;

                while (!flushOnLaunchCompleted && waitMs < LAUNCH_CRASH_TIMEOUT_MS) {
                    try {
                        Thread.sleep(LAUNCH_CRASH_POLL_MS);
                        waitMs += LAUNCH_CRASH_POLL_MS;
                    } catch (InterruptedException exception) {
                        logger.w("Interrupted while waiting for launch crash report request");
                    }
                }
                logger.i("Continuing with Bugsnag initialisation");
            }
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
            logger.w("Failed to flush all on-disk errors, retaining unsent errors for later.");
        }
    }

    void flushReports(Collection<File> storedReports) {
        if (!storedReports.isEmpty() && semaphore.tryAcquire(1)) {
            try {
                logger.i(String.format(Locale.US,
                    "Sending %d saved error(s) to Bugsnag", storedReports.size()));

                for (File eventFile : storedReports) {
                    flushEventFile(eventFile);
                }
            } finally {
                semaphore.release(1);
            }
        }
    }

    private void flushEventFile(File eventFile) {
        try {
            EventFilenameInfo eventInfo = EventFilenameInfo.Companion.fromFile(eventFile, config);
            String apiKey = eventInfo.getApiKey();
            EventPayload payload = new EventPayload(apiKey, eventFile, notifier, config);
            DeliveryParams deliveryParams = config.getErrorApiDeliveryParams(payload);
            DeliveryStatus deliveryStatus = config.getDelivery().deliver(payload, deliveryParams);

            switch (deliveryStatus) {
                case DELIVERED:
                    deleteStoredFiles(Collections.singleton(eventFile));
                    logger.i("Deleting sent error file " + eventFile.getName());
                    break;
                case UNDELIVERED:
                    cancelQueuedFiles(Collections.singleton(eventFile));
                    logger.w("Could not send previously saved error(s)"
                            + " to Bugsnag, will try again later");
                    break;
                case FAILURE:
                    Exception exc = new RuntimeException("Failed to deliver event payload");
                    handleEventFlushFailure(exc, eventFile);
                    break;
                default:
                    break;
            }
        } catch (Exception exception) {
            handleEventFlushFailure(exception, eventFile);
        }
    }

    private void handleEventFlushFailure(Exception exc, File eventFile) {
        if (delegate != null) {
            delegate.onErrorIOFailure(exc, eventFile, "Crash Report Deserialization");
        }
        deleteStoredFiles(Collections.singleton(eventFile));
    }

    private List<File> findLaunchCrashReports(Collection<File> storedFiles) {
        List<File> launchCrashes = new ArrayList<>();

        for (File file : storedFiles) {
            EventFilenameInfo filenameInfo = EventFilenameInfo.Companion.fromFile(file, config);
            if (filenameInfo.isLaunchCrashReport()) {
                launchCrashes.add(file);
            }
        }
        return launchCrashes;
    }

    @NonNull
    @Override
    String getFilename(Object object) {
        EventFilenameInfo eventInfo
                = EventFilenameInfo.Companion.fromEvent(object, null, config);
        String encodedInfo = eventInfo.encode();
        return String.format(Locale.US, "%s%s", storeDirectory, encodedInfo);
    }

    String getNdkFilename(Object object, String apiKey) {
        EventFilenameInfo eventInfo
                = EventFilenameInfo.Companion.fromEvent(object, apiKey, config);
        String encodedInfo = eventInfo.encode();
        return String.format(Locale.US, "%s%s", storeDirectory, encodedInfo);
    }
}
