package com.bugsnag.android;

import static com.bugsnag.android.HandledState.REASON_UNHANDLED_EXCEPTION;
import static com.bugsnag.android.ImmutableConfig.HEADER_API_KEY;
import static com.bugsnag.android.ImmutableConfig.HEADER_INTERNAL_ERROR;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

class InternalReportDelegate implements EventStore.Delegate {

    static final String INTERNAL_DIAGNOSTICS_TAB = "BugsnagDiagnostics";

    final Logger logger;
    final ImmutableConfig immutableConfig;
    final StorageManager storageManager;

    final AppDataCollector appDataCollector;
    final DeviceDataCollector deviceDataCollector;
    final Context appContext;
    final SessionTracker sessionTracker;
    final Notifier notifier;

    InternalReportDelegate(Context context,
                           Logger logger,
                           ImmutableConfig immutableConfig,
                           StorageManager storageManager,
                           AppDataCollector appDataCollector,
                           DeviceDataCollector deviceDataCollector,
                           SessionTracker sessionTracker,
                           Notifier notifier) {
        this.logger = logger;
        this.immutableConfig = immutableConfig;
        this.storageManager = storageManager;
        this.appDataCollector = appDataCollector;
        this.deviceDataCollector = deviceDataCollector;
        this.appContext = context;
        this.sessionTracker = sessionTracker;
        this.notifier = notifier;
    }

    @Override
    public void onErrorIOFailure(Exception exc, File errorFile, String context) {
        // send an internal error to bugsnag with no cache
        HandledState handledState = HandledState.newInstance(REASON_UNHANDLED_EXCEPTION);
        Event err = new Event(exc, immutableConfig, handledState, logger);
        err.setContext(context);

        err.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "canRead", errorFile.canRead());
        err.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "canWrite", errorFile.canWrite());
        err.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "exists", errorFile.exists());

        @SuppressLint("UsableSpace") // storagemanager alternative API requires API 26
        long usableSpace = appContext.getCacheDir().getUsableSpace();
        err.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "usableSpace", usableSpace);
        err.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "filename", errorFile.getName());
        err.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "fileLength", errorFile.length());
        recordStorageCacheBehavior(err);
        reportInternalBugsnagError(err);
    }

    void recordStorageCacheBehavior(Event event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            File cacheDir = appContext.getCacheDir();
            File errDir = new File(cacheDir, "bugsnag-errors");

            try {
                boolean tombstone = storageManager.isCacheBehaviorTombstone(errDir);
                boolean group = storageManager.isCacheBehaviorGroup(errDir);
                event.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "cacheTombstone", tombstone);
                event.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "cacheGroup", group);
            } catch (IOException exc) {
                logger.w("Failed to record cache behaviour, skipping diagnostics", exc);
            }
        }
    }

    /**
     * Reports an event that occurred within the notifier to bugsnag. A lean event report will be
     * generated and sent asynchronously with no callbacks, retry attempts, or writing to disk.
     * This is intended for internal use only, and reports will not be visible to end-users.
     */
    void reportInternalBugsnagError(@NonNull Event event) {
        event.setApp(appDataCollector.generateAppWithState());
        event.setDevice(deviceDataCollector.generateDeviceWithState(new Date().getTime()));

        event.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "notifierName", notifier.getName());
        event.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "notifierVersion", notifier.getVersion());
        event.addMetadata(INTERNAL_DIAGNOSTICS_TAB, "apiKey", immutableConfig.getApiKey());

        final EventPayload eventPayload = new EventPayload(null, event, notifier);
        try {
            Async.run(new Runnable() {
                @Override
                public void run() {
                    try {
                        Delivery delivery = immutableConfig.getDelivery();
                        DeliveryParams params = immutableConfig.getErrorApiDeliveryParams();

                        // can only modify headers if DefaultDelivery is in use
                        if (delivery instanceof DefaultDelivery) {
                            Map<String, String> headers = params.getHeaders();
                            headers.put(HEADER_INTERNAL_ERROR, "true");
                            headers.remove(HEADER_API_KEY);
                            DefaultDelivery defaultDelivery = (DefaultDelivery) delivery;
                            defaultDelivery.deliver(params.getEndpoint(), eventPayload, headers);
                        }

                    } catch (Exception exception) {
                        logger.w("Failed to report internal event to Bugsnag", exception);
                    }
                }
            });
        } catch (RejectedExecutionException ignored) {
            // drop internal report
        }
    }
}
