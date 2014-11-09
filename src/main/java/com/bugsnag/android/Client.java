package com.bugsnag.android;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;

public class Client {
    private Context appContext;
    private Configuration config;

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

        // Install a default exception handler with this client
        if(installHandler) {
            ExceptionHandler.install(this);
        }

        // TODO: Make metrics request
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
        config.setUser(id, email, name);
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
        final Error error = new Error(config, exception, severity, metaData);
        if(error.shouldIgnore()) return;

        // TODO: Run beforeNotify callbacks

        // Build the notification
        final Notification notification = new Notification(config);
        notification.addError(error);

        // Attempt to send the notification in the background
        Async.run(new Runnable() {
            @Override
            public void run() {
                try {
                    notification.deliver();
                    Logger.info("Sent error(s) to Bugsnag");
                } catch (IOException e) {
                    Logger.info("Could not send error(s) to Bugsnag, saving to disk to send later");
                    // TODO: Save to disk
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
