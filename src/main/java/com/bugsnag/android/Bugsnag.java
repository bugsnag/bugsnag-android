package com.bugsnag.android;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.bugsnag.Client;
import com.bugsnag.Error;
import com.bugsnag.MetaData;
import com.bugsnag.Metrics;
import com.bugsnag.Notification;
import com.bugsnag.http.HttpClient;
import com.bugsnag.http.NetworkException;
import com.bugsnag.http.BadResponseException;
import com.bugsnag.utils.JSONUtils;

public class Bugsnag {
    private static final String PREFS_NAME = "Bugsnag";
    private static final String UNSENT_ERROR_PATH = "/bugsnag-errors/";
    private static final String DEFAULT_METRICS_ENDPOINT = "notify.bugsnag.com/metrics";

    private static final String NOTIFIER_NAME = "Android Bugsnag Notifier";
    private static final String NOTIFIER_VERSION = "2.0.0";

    private static Context applicationContext;
    private static String cachePath;
    private static Logger logger;
    private static Client client;
    private static String uuid;
    private static boolean enableMetrics = false;
    private static String metricsEndpoint = DEFAULT_METRICS_ENDPOINT;

    public static void register(Context androidContext, String apiKey) {
        register(androidContext, apiKey, false);
    }

    public static void register(Context androidContext, String apiKey, boolean enableMetrics) {
        // Create a logger
        logger = new Logger();

        // Create the bugsnag client
        client = new Client(apiKey);
        client.setLogger(logger);

        // Get the application context, many things need this
        applicationContext = androidContext.getApplicationContext();
        cachePath = prepareCachePath();

        // Get the uuid for metrics and userId
        String uuid = getUUID();

        // Get package information
        String packageName = getPackageName();
        String packageVersion = getPackageVersion(packageName);

        // Set notifier info
        client.setNotifierName(NOTIFIER_NAME);
        client.setNotifierVersion(NOTIFIER_VERSION);

        // Set common meta-data
        client.setUserId(uuid);
        client.setOsVersion(android.os.Build.VERSION.RELEASE);
        client.setAppVersion(packageVersion);
        client.setProjectPackages(packageName);

        client.addToTab("Device", "Android Version", android.os.Build.VERSION.RELEASE);
        client.addToTab("Device", "Device Type", android.os.Build.MODEL);

        client.addToTab("Application", "Package Name", packageName);
        client.addToTab("Application", "Package Version", packageVersion);

        // Send metrics data (DAU/MAU etc) if enabled
        if(enableMetrics) {
            makeMetricsRequest(uuid);
        }

        // Flush any queued exceptions
        flushErrors();

        logger.info("Bugsnag is loaded and ready to handle exceptions");
    }

    public static void notify(Throwable e) {
        notify(e, null);
    }

    public static void notify(Throwable e, MetaData overrides) {
        if(client == null) {
            Log.e("Bugsnag", "You must call register with an apiKey before we can notify of exceptions!");
            return;
        }

        // Generate diagnostic data
        MetaData diagnostics = new Diagnostics(applicationContext);

        // Merge local metaData into diagnostics
        MetaData metaData = diagnostics.merge(overrides);

        // Create the error object to send
        final Error error = client.createError(e, metaData);

        // Set the error's context
        String topActivityName = ActivityStack.getTopActivityName();
        if(topActivityName != null) {
            error.setContext(topActivityName);
        }

        // Send the error
        new AsyncTask <Void, Void, Void>() {
            protected Void doInBackground(Void... voi) {
                try {
                    Notification notif = client.createNotification(error);
                    notif.deliver();
                } catch (NetworkException ex) {
                    // Write error to disk for later sending
                    logger.info("Could not send error(s) to Bugsnag, saving to disk to send later");
                    writeErrorToDisk(error);
                } catch (BadResponseException ex) {
                    // The notification was delivered, but Bugsnag sent a non-200 response
                    logger.warn(ex.getMessage());
                }

                return null;
            }
        }.execute();
    }

    public static void setContext(String context) {
        client.setContext(context);
    }

    public static void setContext(Activity context) {
        String contextString = ActivityStack.getContextName(context);
        client.setContext(contextString);
    }

    public static void setUserId(String userId) {
        client.setUserId(userId);
    }

    public static void setReleaseStage(String releaseStage) {
        client.setReleaseStage(releaseStage);
    }

    public static void setNotifyReleaseStages(String... notifyReleaseStages) {
        client.setNotifyReleaseStages(notifyReleaseStages);
    }

    public static void setAutoNotify(boolean autoNotify) {
        client.setAutoNotify(autoNotify);
    }

    public static void setUseSSL(boolean useSSL) {
        client.setUseSSL(useSSL);
    }

    public static void setEndpoint(String endpoint) {
        client.setEndpoint(endpoint);
    }

    public static void setMetricsEndpoint(String metricsEndpoint) {
        Bugsnag.metricsEndpoint = metricsEndpoint;
    }

    public static String getMetricsEndpoint() {
        return (client.getUseSSL() ? "https://" : "http://") + metricsEndpoint;
    }

    public static void addToTab(String tab, String key, Object value) {
        client.addToTab(tab, key, value);
    }

    private static void flushErrors() {
        if(cachePath == null) return;

        new AsyncTask <Void, Void, Void>() {
            protected Void doInBackground(Void... voi) {
                logger.debug("Flushing unsent errors (if any)");

                // Create a notification
                Notification notif = client.createNotification();
                List<File> sentFiles = new LinkedList<File>();

                // Look up all saved error files
                File exceptionDir = new File(cachePath);
                if(exceptionDir.exists() && exceptionDir.isDirectory()) {
                    for(File errorFile : exceptionDir.listFiles()) {
                        if(errorFile.exists() && errorFile.isFile()) {
                            // Save filename in a "to delete" array
                            sentFiles.add(errorFile);

                            try {
                                // Read error from disk and add to notification
                                String errorString = FileUtils.readFileAsString(errorFile);
                                notif.addError(errorString);

                                logger.debug(String.format("Added unsent error (%s) to notification", errorFile.getName()));
                            } catch (IOException e) {
                                logger.warn("Problem reading unsent error from disk", e);
                            }
                        }
                    }
                }

                try {
                    // Send the notification
                    notif.deliver();

                    // Delete the files if notification worked
                    for(File file : sentFiles) {
                        logger.debug("Deleting unsent error file " + file.getName());
                        file.delete();
                    }
                } catch (IOException e) {
                    logger.info("Could not flush error(s) to Bugsnag, will try again later");
                } catch (BadResponseException ex) {
                    // The notification was delivered, but Bugsnag sent a non-200 response
                    logger.warn(ex.getMessage());
                }

                return null;
            }
        }.execute();
    }

    private static String getPackageName() {
        return applicationContext.getPackageName();
    }

    private static String getPackageVersion(String packageName) {
        String packageVersion = null;

        try {
            PackageInfo pi = applicationContext.getPackageManager().getPackageInfo(packageName, 0);
            packageVersion = pi.versionName;
        } catch(Exception e) {
            logger.warn("Could not get package version", e);
        }

        return packageVersion;
    }

    private static String prepareCachePath() {
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

    // TODO:JS Avoid StrictMode violations caused by getSharedPreferences
    // TODO:JS Avoid StrictMode violations caused by UUID.randomUUID
    private static String getUUID() {
        final SharedPreferences settings = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uuid = settings.getString("userId", null);
        if(uuid == null) {
            uuid = UUID.randomUUID().toString();

            // Save if for future
            final String finalUuid = uuid;
            new AsyncTask <Void, Void, Void>() {
                protected Void doInBackground(Void... voi) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("userId", finalUuid);
                    editor.commit();

                    return null;
                }
            }.execute();
        }
        return uuid;
    }

    private static void writeErrorToDisk(Error error) {
        if(cachePath == null) return;

        String errorString = error.toString();
        if(!errorString.isEmpty()) {
            // Write the error to disk
            String filename = String.format("%s%d.json", cachePath, System.currentTimeMillis());
            try {
                FileUtils.writeStringToFile(errorString, filename);
                logger.debug(String.format("Saved unsent error to disk (%s) ", filename));
            } catch (IOException e) {
                logger.warn("Could not save error to disk", e);
            }
        }
    }

    private static void makeMetricsRequest(final String userId) {
        new AsyncTask <Void, Void, Void>() {
            protected Void doInBackground(Void... voi) {
                try {
                    Metrics metrics = client.createMetrics(userId);
                    metrics.deliver();
                } catch (NetworkException ex) {
                    // Write error to disk for later sending
                    logger.info("Could not send metrics to Bugsnag");
                } catch (BadResponseException ex) {
                    // The notification was delivered, but Bugsnag sent a non-200 response
                    logger.warn(ex.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        }.execute();
    }
}