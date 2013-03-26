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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.provider.Settings.Secure;

import com.bugsnag.Error;
import com.bugsnag.MetaData;
import com.bugsnag.Metrics;
import com.bugsnag.Notification;
import com.bugsnag.http.HttpClient;
import com.bugsnag.http.NetworkException;
import com.bugsnag.http.BadResponseException;
import com.bugsnag.utils.JSONUtils;

public class Client extends com.bugsnag.Client {
    private static final String PREFS_NAME = "Bugsnag";
    private static final String UNSENT_ERROR_PATH = "/bugsnag-errors/";
    private static final String NOTIFIER_NAME = "Android Bugsnag Notifier";
    private static final String NOTIFIER_VERSION = "2.0.6";

    private Context applicationContext;
    private String cachePath;
    private String uuid;

    public Client(Context androidContext, String apiKey, boolean enableMetrics) {
        super(apiKey);

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
        setNotifyReleaseStages("production", "development");

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

        config.logger.info("Bugsnag is loaded and ready to handle exceptions");
    }

    public void notify(Throwable e) {
        notify(e, null);
    }

    public void notify(Throwable e, MetaData overrides) {
        try {
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
            safeAsync(new AnonymousDelegate(){
                public void perform() {
                    try {
                        Notification notif = createNotification(error);
                        notif.deliver();
                    } catch (NetworkException ex) {
                        // Write error to disk for later sending
                        config.logger.info("Could not send error(s) to Bugsnag, saving to disk to send later");
                        writeErrorToDisk(error);
                    }
                }
            });
        } catch(Exception ex) {
            config.logger.warn("Error notifying Bugsnag", ex);
        }
    }

    private void flushErrors() {
        if(cachePath == null) return;

        safeAsync(new AnonymousDelegate(){
            public void perform() {
                // Create a notification
                Notification notif = createNotification();
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
                                String errorString = Utils.readFileAsString(errorFile);
                                notif.addError(errorString);

                                config.logger.debug(String.format("Added unsent error (%s) to notification", errorFile.getName()));
                            } catch (IOException e) {
                                config.logger.warn("Problem reading unsent error from disk", e);
                            }
                        }
                    }
                }

                try {
                    // Send the notification
                    notif.deliver();

                    // Delete the files if notification worked
                    for(File file : sentFiles) {
                        config.logger.debug("Deleting unsent error file " + file.getName());
                        file.delete();
                    }
                } catch (IOException e) {
                    config.logger.info("Could not flush error(s) to Bugsnag, will try again later");
                }
            }
        });
    }

    public void setContext(Activity context) {
        String contextString = ActivityStack.getContextName(context);
        setContext(contextString);
    }

    private void makeMetricsRequest(final String userId) {
        safeAsync(new AnonymousDelegate(){
            public void perform() {
                try {
                    Metrics metrics = createMetrics(userId);
                    metrics.deliver();
                } catch (NetworkException ex) {
                    // Write error to disk for later sending
                    config.logger.info("Could not send metrics to Bugsnag");
                } catch (BadResponseException ex) {
                    // The notification was delivered, but Bugsnag sent a non-200 response
                    config.logger.warn(ex.getMessage());
                } catch (SecurityException ex) {
                    //TODO
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
            config.logger.warn("Could not guess release stage", e);
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
                config.logger.warn("Could not prepare cache directory");
                path = null;
            }
        } catch(Exception e) {
            config.logger.warn("Could not prepare cache directory", e);
            path = null;
        }

        return path;
    }

    private void writeErrorToDisk(Error error) {
        if(cachePath == null) return;

        String errorString = error.toString();
        if(errorString.length() > 0) {
            // Write the error to disk
            String filename = String.format("%s%d.json", cachePath, System.currentTimeMillis());
            try {
                Utils.writeStringToFile(errorString, filename);
                config.logger.debug(String.format("Saved unsent error to disk (%s) ", filename));
            } catch (IOException e) {
                config.logger.warn("Could not save error to disk", e);
            }
        }
    }

    private String getUUID() {
        String uuid = Secure.getString(applicationContext.getContentResolver(), Secure.ANDROID_ID);;
        if(uuid == null) {
            final SharedPreferences settings = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            uuid = settings.getString("userId", null);
            if(uuid == null) {
                uuid = UUID.randomUUID().toString();

                // Save if for future
                final String finalUuid = uuid;

                safeAsync(new AnonymousDelegate(){
                    public void perform() {
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("userId", finalUuid);
                        editor.commit();
                    }
                });
            }
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
            config.logger.warn("Could not get package version", e);
        }

        return packageVersion;
    }

    interface AnonymousDelegate {
        void perform();
    }

    private void safeAsync(final AnonymousDelegate delegate) {
        new AsyncTask <Void, Void, Void>() {
            protected Void doInBackground(Void... voi) {
                try {
                    delegate.perform();
                } catch (Exception e) {
                    config.logger.warn("Error in bugsnag", e);
                }

                return null;
            }
        }.execute();
    }
}