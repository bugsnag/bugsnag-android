package com.bugsnag.android;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.bugsnag.Error;
import com.bugsnag.Notification;
import com.bugsnag.MetaData;

public class Client extends com.bugsnag.Client {
    private static final String PREFS_NAME = "Bugsnag";
    private static final String UNSENT_ERROR_PATH = "/bugsnag-errors/";

    private Context applicationContext;
    private String packageName;
    private String packageVersion;
    private String cachePath;

    public Client(Context androidContext, String apiKey) {
        // Set the apiKey and logger
        super(apiKey);
        setLogger(new Logger());

        // Get the application context, many things need this
        applicationContext = androidContext.getApplicationContext();
        packageName = getPackageName();
        packageVersion = getPackageVersion(packageName);
        cachePath = prepareCachePath();

        // Set common meta-data
        setUserId(getUUID());
        setOsVersion(android.os.Build.VERSION.RELEASE);
        setAppVersion(packageVersion);
        setProjectPackages(packageName);

        addToTab("Device", "Android Version", android.os.Build.VERSION.RELEASE);
        addToTab("Device", "Device Type", android.os.Build.MODEL);

        addToTab("Application", "Package Name", packageName);
        addToTab("Application", "Package Version", packageVersion);

        // Flush any queued exceptions
        flush();

        config.getLogger().info("Bugsnag is loaded and ready to handle exceptions");
    }

    @Override
    public void notify(final Throwable e, MetaData overrides) {
        // Generate diagnostic data
        MetaData diagnostics = new Diagnostics(applicationContext);

        // Merge local metaData into diagnostics
        MetaData metaData = diagnostics.merge(overrides);

        // Create the error object to send
        final Error error = new Error(e, metaData, config);

        // Set the error's context
        String topActivityName = ActivityStack.getTopActivityName();
        if(topActivityName != null) {
            error.setContext(topActivityName);
        }

        // Send the error
        new Thread(new Runnable() {
            public void run() {
                try {
                    Notification notif = new Notification(config, error);
                    notif.deliver();
                } catch (IOException ex) {
                    // Write error to disk for later sending
                    if(cachePath != null) {
                        config.getLogger().info("Could not send error(s) to Bugsnag, saving to disk to send later");
                        writeErrorToDisk(error);
                    }
                }
            }
        }).start();
    }

    public void setContext(Activity context) {
        setContext(ActivityStack.getContextName(context));
    }

    private void flush() {
        new Thread(new Runnable() {
            public void run() {
                if(cachePath != null) {
                    config.getLogger().debug("Flushing unsent errors (if any)");

                    // Create a notification
                    Notification notif = new Notification(config);
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

                                    config.getLogger().debug(String.format("Added unsent error (%s) to notification", errorFile.getName()));
                                } catch (IOException e) {
                                    config.getLogger().warn("Problem reading unsent error from disk", e);
                                }
                            }
                        }
                    }

                    // Send the notification
                    try {
                        notif.deliver();

                        // Delete the files if notification worked
                        for(File file : sentFiles) {
                            config.getLogger().debug("Deleting unsent error file " + file.getName());
                            file.delete();
                        }
                    } catch (IOException e) {
                        config.getLogger().info("Could not send error(s) to Bugsnag, will try again later");
                    }
                }
            }
        }).start();
    }

    private String getUUID() {

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

    private String getPackageName() {
        return applicationContext.getPackageName();
    }

    private String getPackageVersion(String packageName) {
        String packageVersion = null;

        try {
            PackageInfo pi = applicationContext.getPackageManager().getPackageInfo(packageName, 0);
            packageVersion = pi.versionName;
        } catch(Exception e) {
            config.getLogger().warn("Could not get package version", e);
        }

        return packageVersion;
    }

    private String prepareCachePath() {
        String path = null;

        try {
            path = applicationContext.getCacheDir().getAbsolutePath() + UNSENT_ERROR_PATH;

            File outFile = new File(path);
            outFile.mkdirs();
            if(!outFile.exists()) {
                config.getLogger().warn("Could not prepare cache directory");
                path = null;
            }
        } catch(Exception e) {
            config.getLogger().warn("Could not prepare cache directory", e);
            path = null;
        }

        return path;
    }

    private void writeErrorToDisk(Error error) {
        String errorString = error.toString();
        if(!errorString.isEmpty()) {
            // Write the error to disk
            String filename = String.format("%s%d.json", cachePath, System.currentTimeMillis());
            try {
                FileUtils.writeStringToFile(errorString, filename);
                config.getLogger().debug(String.format("Saved unsent error to disk (%s) ", filename));
            } catch (IOException e) {
                config.getLogger().warn("Could not save error to disk", e);
            }
        }
    }
}