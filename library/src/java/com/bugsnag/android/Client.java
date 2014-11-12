package com.bugsnag.android;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;

public class Client {
    private Context appContext;
    private Configuration config;
    private Diagnostics diagnostics;
    private ErrorStore errorStore;

    public Client(Context androidContext, String apiKey) {
        this(androidContext, apiKey, true, true);
    }

    public Client(Context androidContext, String apiKey, boolean enableMetrics) {
        this(androidContext, apiKey, enableMetrics, true);
    }

    public Client(Context androidContext, String apiKey, boolean enableMetrics, boolean installHandler) {
        if(androidContext == null) {
            throw new RuntimeException("You must provide a non-null android Context");
        }

        if(apiKey == null) {
            throw new RuntimeException("You must provide a Bugsnag API key");
        }

        // Build a configuration object
        config = new Configuration(apiKey);

        // Get the application context, many things need this
        appContext = androidContext.getApplicationContext();

        // Set up in-project detection
        setProjectPackages(appContext.getPackageName());

        // Set up diagnostics collection
        diagnostics = new Diagnostics(config, appContext);

        // Flush any on-disk errors
        errorStore = new ErrorStore(config, appContext);
        errorStore.flush();

        // Install a default exception handler with this client
        if(installHandler) {
            ExceptionHandler.install(this);
        }

        // Make metrics request
        Async.run(new Runnable() {
            @Override
            public void run() {
                try {
                    new Metrics(config, diagnostics).deliver();
                    Logger.info("Sent metrics to Bugsnag");
                } catch (IOException e) {
                    Logger.info("Could not send metrics to Bugsnag");
                }
            }
        });
    }

    public void setAppVersion(String appVersion) {
        config.appVersion = appVersion;
    }

    public void setAutoNotify(boolean autoNotify) {
        config.autoNotify = autoNotify;
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

    public void notify(Throwable e) {
        notify(e, null, null);
    }

    public void notify(Throwable e, Severity severity) {
        notify(e, severity, null);
    }

    public void notify(Throwable e, MetaData metaData) {
        notify(e, null, metaData);
    }

    public void notify(Throwable exception, Severity severity, MetaData metaData) {
        final Error error = new Error(config, diagnostics, exception, severity, metaData);

        // Don't notify if this error class should be ignored or release stage is blocked
        if(error.shouldIgnore()) return;

        // Run beforeNotify tasks, don't notify if any return true
        if(!config.runBeforeNotify(error)) {
            Logger.info("Skipping notification - beforeNotify task returned false");
            return;
        }

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

    public void autoNotify(Throwable e) {
        if(config.autoNotify) {
            notify(e, Severity.ERROR);
        }
    }

    public void addToTab(String tab, String key, Object value) {
        config.addToTab(tab, key, value);
    }

    public void clearTab(String tab) {
        config.clearTab(tab);
    }
}
