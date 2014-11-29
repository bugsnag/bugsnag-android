package com.bugsnag.android;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;

public class Client {
    private Configuration config;
    private Diagnostics diagnostics;
    private ErrorStore errorStore;
    private boolean sentAnalytics = false;

    public Client(Context androidContext, String apiKey) {
        this(androidContext, apiKey, true, true);
    }

    public Client(Context androidContext, String apiKey, boolean sendAnalytics) {
        this(androidContext, apiKey, sendAnalytics, true);
    }

    public Client(Context androidContext, String apiKey, boolean sendAnalytics, boolean enableExceptionHandler) {
        if(androidContext == null) {
            throw new RuntimeException("You must provide a non-null android Context");
        }

        if(apiKey == null) {
            throw new RuntimeException("You must provide a Bugsnag API key");
        }

        // Build a configuration object
        config = new Configuration(apiKey);

        // Get the application context, many things need this
        Context appContext = androidContext.getApplicationContext();

        // Set up in-project detection
        setProjectPackages(appContext.getPackageName());

        // Set up diagnostics collection
        diagnostics = new Diagnostics(config, appContext);

        // Flush any on-disk errors
        errorStore = new ErrorStore(config, appContext);
        errorStore.flush();

        // Install a default exception handler with this client
        if(enableExceptionHandler) {
            enableExceptionHandler();
        }

        // Make analytics request
        if(sendAnalytics) {
            sendAnalytics();
        }
    }

    public void setAppVersion(String appVersion) {
        config.appVersion = appVersion;
    }

    public void setContext(String context) {
        config.context = context;
    }

    public void setEndpoint(String endpoint) {
        config.endpoint = endpoint;
    }

    public void setFilters(String... filters) {
        config.filters = filters;
    }

    public void setIgnoreClasses(String... ignoreClasses) {
        config.ignoreClasses = ignoreClasses;
    }

    public void setNotifyReleaseStages(String... notifyReleaseStages) {
        config.notifyReleaseStages = notifyReleaseStages;
    }

    public void setProjectPackages(String... projectPackages) {
        config.projectPackages = projectPackages;
    }

    public void setReleaseStage(String releaseStage) {
        config.releaseStage = releaseStage;
    }

    public void setSendThreads(boolean sendThreads) {
        config.sendThreads = sendThreads;
    }

    public void setUser(String id, String email, String name) {
        // TODO
    }

    public void addBeforeNotify(BeforeNotify beforeNotify) {
        config.addBeforeNotify(beforeNotify);
    }

    public void notify(Throwable exception) {
        Error error = new Error(config, exception);
        notify(error);
    }

    public void notify(Throwable exception, Severity severity) {
        Error error = new Error(config, exception);
        error.setSeverity(severity);
        notify(error);
    }

    public void notify(Throwable exception, MetaData metaData) {
        Error error = new Error(config, exception);
        error.setMetaData(metaData);
        notify(error);
    }

    public void notify(Throwable exception, Severity severity, MetaData metaData) {
        Error error = new Error(config, exception);
        error.setSeverity(severity);
        error.setMetaData(metaData);
        notify(error);
    }

    public void notify(final Error error) {
        // Don't notify if this error class should be ignored
        if(error.shouldIgnoreClass()) {
            return;
        }

        // Don't notify unless releaseStage is in notifyReleaseStages
        if(!config.shouldNotifyForReleaseStage(diagnostics.getReleaseStage())) {
            return;
        }

        // Run beforeNotify tasks, don't notify if any return true
        if(!BeforeNotify.runAll(config.beforeNotifyTasks, error)) {
            Logger.info("Skipping notification - beforeNotify task returned false");
            return;
        }

        // Attach diagnostic info to the error
        error.setDiagnostics(diagnostics);

        // Build the notification
        final Notification notification = new Notification(config);
        notification.addError(error);

        // Attempt to send the notification in the background
        Async.run(new Runnable() {
            @Override
            public void run() {
                try {
                    int errorCount = notification.deliver();
                    Logger.info(String.format("Sent %d new error(s) to Bugsnag", errorCount));
                } catch (IOException e) {
                    Logger.info("Could not send error(s) to Bugsnag, saving to disk to send later");

                    // Save error to disk for later sending
                    errorStore.write(error);
                }
            }
        });
    }

    public void addToTab(String tab, String key, Object value) {
        config.addToTab(tab, key, value);
    }

    public void clearTab(String tab) {
        config.clearTab(tab);
    }

    public void sendAnalytics() {
        // Never send analytics twice per session
        if(sentAnalytics) {
            return;
        }

        // Make the analytics request in the background
        Async.run(new Runnable() {
            @Override
            public void run() {
                try {
                    new Metrics(config, diagnostics).deliver();
                    Logger.info("Sent analytics data to Bugsnag");
                } catch (IOException e) {
                    Logger.info("Could not send analytics data to Bugsnag");
                }
            }
        });

        sentAnalytics = true;
    }

    public void enableExceptionHandler() {
        ExceptionHandler.enable(this);
    }

    public void disableExceptionHandler() {
        ExceptionHandler.disable(this);
    }
}
