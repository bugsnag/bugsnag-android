package com.bugsnag.android;

import java.io.File;
import java.io.FileWriter;
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
import android.os.SystemClock;

import com.bugsnag.Error;
import com.bugsnag.Notification;
import com.bugsnag.MetaData;

public class Client extends com.bugsnag.Client {
    private static final String PREFS_NAME = "Bugsnag";
    private static final String UNSENT_ERROR_PATH = "/unsent_errors/";

    private Context applicationContext;
    private String packageName;
    private String packageVersion;
    private String cachePath;
    private long startTime;

    public Client(Context androidContext, String apiKey) {
        // Set the apiKey and logger
        super(apiKey);
        setLogger(new Logger());

        // Get the application context, many things need this
        applicationContext = androidContext.getApplicationContext();
        packageName = getPackageName();
        packageVersion = getPackageVersion(packageName);
        cachePath = prepareCachePath();
        startTime = secondsSinceBoot();

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

        config.getLogger().info("Ready to handle exceptions.");
    }

    @Override
    public void notify(final Throwable e, MetaData overrides) {
        // Generate diagnostic data
        MetaData diagnostics = generateDiagnostics();

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
                Notification notif = new Notification(config, error);
                boolean sent = notif.deliver();

                // Write error to disk for later sending
                if(!sent && cachePath != null) {
                    config.getLogger().info("Could not deliver error notification, saving to disk to send later.");
                    writeErrorToDisk(error);
                }
            }
        }).start();
    }

    public void setContext(Activity context) {
        setContext(Util.getContextName(context));
    }

    private void flush() {
        new Thread(new Runnable() {
            public void run() {
                if(cachePath != null) {
                    config.getLogger().debug("Flushing cached errors");

                    // Create a notification
                    Notification notif = new Notification(config);
                    List<File> sentFiles = new LinkedList<File>();

                    // Look up all saved error files
                    File exceptionDir = new File(cachePath);
                    if(exceptionDir.exists() && exceptionDir.isDirectory()) {
                        File[] errorFiles = exceptionDir.listFiles();
                        for(File errorFile : errorFiles) {
                            if(errorFile.exists() && errorFile.isFile()) {
                                // Save filename in a "to delete" array
                                sentFiles.add(errorFile);
                                System.out.println(String.format("DEBUG: File is %d bytes long", errorFile.length()));

                                // Read file into string
                                String errorString = Util.readFileAsString(errorFile);

                                // Add errorString to notification
                                notif.addError(errorString);

                                config.getLogger().debug("Added error file to notification " + errorFile.getName());
                            }
                        }
                    }

                    // Send the notification
                    boolean sent = notif.deliver();
                    if(sent) {
                        // Delete the files if notification worked
                        for(File file : sentFiles) {
                            config.getLogger().debug("Deleting error file " + file.getName());
                            file.delete();
                        }
                    } else {
                        config.getLogger().info("Could not deliver error notification, will try again later.");
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
                config.getLogger().warn("Error preparing cache directory");
                path = null;
            }
        } catch(Exception e) {
            config.getLogger().warn("Error preparing cache directory", e);
            path = null;
        }

        return path;
    }

    private long secondsSinceBoot() {
        return (long)(SystemClock.elapsedRealtime()/1000);
    }

    private MetaData generateDiagnostics() {
        MetaData diagnostics = new MetaData();

        // Activity stack
        String topActivityName = ActivityStack.getTopActivityName();
        List<String> activityStackNames = ActivityStack.getNames();
        if(activityStackNames.size() > 0) {
            diagnostics.addToTab("Application", "Activity Stack", activityStackNames);
        }

        if(topActivityName != null) {
            diagnostics.addToTab("Application", "Top Activity", topActivityName);
        }
 
        // Session information
        diagnostics.addToTab("Session", "Session Length", String.format("%d seconds", secondsSinceBoot() - startTime));

        // Device state
        diagnostics.addToTab("Device", "Seconds Since Boot", String.format("%d seconds", secondsSinceBoot()));
        diagnostics.addToTab("Device", "GPS State", "TODO");
        diagnostics.addToTab("Device", "Network State", "TODO");
        diagnostics.addToTab("Device", "Free Memory", "TODO");

        return diagnostics;
    }

    private void writeErrorToDisk(Error error) {
        String errorString = error.toString();
        if(!errorString.isEmpty()) {
            // Generate a random file name
            int random = new Random().nextInt(99999);
            String filename = String.format("%s%s-%d.json", cachePath, packageVersion, random);

            // Write the error to disk
            FileWriter writer = null;
            try {
                writer = new FileWriter(filename);
                writer.write(errorString);
                writer.flush();

                config.getLogger().debug("Wrote error file to disk: " + filename);
            } catch(IOException ex) {
                config.getLogger().warn("Error when writing exception to disk.", ex);
            } finally {
                if(writer != null) {
                    try { writer.close(); } catch(IOException e) {}
                }
            }
        }
    }
}