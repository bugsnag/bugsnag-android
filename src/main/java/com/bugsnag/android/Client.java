package com.bugsnag.android;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.bugsnag.Error;
import com.bugsnag.MetaData;
import com.bugsnag.Metrics;
import com.bugsnag.Notification;
import com.bugsnag.http.NetworkException;
import com.bugsnag.http.BadResponseException;

public class Client extends com.bugsnag.Client {
    private static final String PREFS_NAME = "Bugsnag";
    private static final String UNSENT_ERROR_PATH = "/bugsnag-errors/";
    private static final String NOTIFIER_NAME = "Android Bugsnag Notifier";
    private static final String NOTIFIER_VERSION = "2.0.9";

    private Logger logger;
    private Context applicationContext;
    private String cachePath;

    public Client(Context androidContext, String apiKey, boolean enableMetrics) {
        super(apiKey);

        // Start the session timer
        Diagnostics.startSessionTimer();

        // Create a logger
        logger = new Logger();
        setLogger(logger);

        // Get the application context, many things need this
        applicationContext = androidContext.getApplicationContext();
        cachePath = prepareCachePath();

        // Get the uuid for metrics and userId
        String uuid = getUUID();

        // Get package information
        String packageName = getPackageName();
        String packageVersion = getPackageVersion(packageName);

        // Set notifier info
        setNotifierName(NOTIFIER_NAME);
        setNotifierVersion(NOTIFIER_VERSION);

        // Set common meta-data
        setUserId(uuid);
        setOsVersion(android.os.Build.VERSION.RELEASE);
        setAppVersion(packageVersion);
        setProjectPackages(packageName);
        setReleaseStage(guessReleaseStage(packageName));

        addToTab("Device", "Android Version", android.os.Build.VERSION.RELEASE);
        addToTab("Device", "Device Type", android.os.Build.MODEL);

        addToTab("Application", "Package Name", packageName);
        addToTab("Application", "Package Version", packageVersion);

        // Send metrics data (DAU/MAU etc) if enabled
        if(enableMetrics) {
            makeMetricsRequest(uuid);
        }

        // Flush any queued exceptions
        flushErrors();

        logger.info("Bugsnag is loaded and ready to handle exceptions");
    }

    public void notify(Throwable e) {
        notify(e, null);
    }

    public void notify(Throwable e, MetaData overrides) {
        try {
            if(!config.shouldNotify()) return;
            if(config.shouldIgnore(e.getClass().getName())) return;

            // Generate diagnostic data
            MetaData diagnostics = new Diagnostics(applicationContext);

            // Merge local metaData into diagnostics
            MetaData metaData = diagnostics.merge(overrides);

            // Create the error object to send
            final Error error = createError(e, metaData);

            // Set the error's context
            String topActivityName = ActivityStack.getTopActivityName();
            if(topActivityName != null) {
                error.setContext(topActivityName);
            }

            // Send the error
            safeAsync(new Runnable() {
                @Override
                public void run() {
                    try {
                        Notification notif = createNotification(error);
                        notif.deliver();
                    } catch (NetworkException ex) {
                        // Write error to disk for later sending
                        logger.info("Could not send error(s) to Bugsnag, saving to disk to send later");
                        writeErrorToDisk(error);
                    }
                }
            });
        } catch(Exception ex) {
            logger.warn("Error notifying Bugsnag", ex);
        }
    }

    private void flushErrors() {
        if(cachePath == null) return;

        safeAsync(new Runnable() {
            @Override
            public void run() {
                // Look up all saved error files
                File exceptionDir = new File(cachePath);
                if(exceptionDir.exists() && exceptionDir.isDirectory()) {
                    Notification notif = null;

                    for(File errorFile : exceptionDir.listFiles()) {
                        try {
                            if(notif == null) notif = createNotification();
                            notif.setError(errorFile);
                            notif.deliver();

                            logger.debug("Deleting sent error file " + errorFile.getName());
                            errorFile.delete();
                        } catch (NetworkException e) {
                            logger.warn("Could not send error(s) to Bugsnag, will try again later", e);
                        } catch (Exception e) {
                            logger.warn("Problem sending unsent error from disk", e);
                            errorFile.delete();
                        }
                    }
                }
            }
        });
    }

    public void setContext(Activity context) {
        String contextString = ActivityStack.getContextName(context);
        setContext(contextString);
    }

    private void makeMetricsRequest(final String userId) {
        safeAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    Metrics metrics = createMetrics(userId);
                    metrics.deliver();
                } catch (NetworkException ex) {
                    // Write error to disk for later sending
                    logger.info("Could not send metrics to Bugsnag");
                } catch (BadResponseException ex) {
                    // The notification was delivered, but Bugsnag sent a non-200 response
                    logger.warn(ex.getMessage());
                }
            }
        });
    }

    private String guessReleaseStage(String packageName) {
        String releaseStage = "production";

        try {
            ApplicationInfo ai = applicationContext.getPackageManager().getApplicationInfo(packageName, 0);
            boolean debuggable = (ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if(debuggable) {
                releaseStage = "development";
            }
        } catch(Exception e) {
            logger.warn("Could not guess release stage", e);
        }

        return releaseStage;
    }

    private String prepareCachePath() {
        String path = null;

        try {
            path = applicationContext.getCacheDir().getAbsolutePath() + UNSENT_ERROR_PATH;

            File outFile = new File(path);
            outFile.mkdirs();
            if(!outFile.exists()) {
                logger.warn("Could not prepare cache directory");
                path = null;
            }
        } catch(Exception e) {
            logger.warn("Could not prepare cache directory", e);
            path = null;
        }

        return path;
    }

    private void writeErrorToDisk(Error error) {
        if(cachePath == null || error == null) return;
        
        try {
            error.writeToFile(String.format("%s%d.json", cachePath, System.currentTimeMillis()));
        } catch (IOException e) {
            logger.warn("Unable to save bugsnag error", e);
        }
    }

    // TODO:JS Avoid StrictMode violations caused by getSharedPreferences
    // TODO:JS Avoid StrictMode violations caused by UUID.randomUUID
    private String getUUID() {
        final SharedPreferences settings = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uuid = settings.getString("userId", null);
        if(uuid == null) {
            uuid = UUID.randomUUID().toString();

            // Save if for future
            final String finalUuid = uuid;

            safeAsync(new Runnable() {
                @Override
                public void run() {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("userId", finalUuid);
                    editor.commit();
                }
            });
        }
        return uuid;
    }

    private String getPackageName() {
        return applicationContext.getPackageName();
    }

    private String getPackageVersion(String packageName) {
        String packageVersion = null;

        try {
            PackageInfo pi = applicationContext.getPackageManager().getPackageInfo(packageName, 0);
            packageVersion = pi.versionName;
        } catch(Exception e) {
            logger.warn("Could not get package version", e);
        }

        return packageVersion;
    }

    private void safeAsync(final Runnable delegate) {
        new AsyncTask <Void, Void, Void>() {
            protected Void doInBackground(Void... voi) {
                try {
                    delegate.run();
                } catch (Exception e) {
                    logger.warn("Error in bugsnag", e);
                }

                return null;
            }
        }.execute();
    }
}
