package com.bugsnag.android;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
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
    private static final String UNSENT_ERROR_PATH = "/unsent_bugsnag_errors/";

    private Context applicationContext;
    private String packageName;
    private String packageVersion;
    private String cachePath;
    private long startTime;
    private List<WeakReference<Context>> activityStack = new LinkedList<WeakReference<Context>>();

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
        // TODO: Async
        // flush();

        config.getLogger().info("Ready to handle exceptions.");
    }

    @Override
    public void notify(final Throwable e, final MetaData metaData) {
        // Add diagnostics to error
        final Error error = new Error(e, metaData, config);

        List<String> activityStackNames = generateActivityStack();
        if(activityStackNames != null && activityStackNames.size() > 0) {
            error.addToTab("Application", "Top Activity", activityStackNames.get(activityStackNames.size() - 1));
            error.addToTab("Application", "Activity Stack", activityStackNames);
        }
        
        error.addToTab("Session", "Session Length", String.format("%d seconds", secondsSinceBoot() - startTime));
        error.addToTab("Device", "Seconds Since Boot", String.format("%d seconds", secondsSinceBoot()));

        // Send the error
        new Thread(new Runnable() {
            public void run() {
                Notification notif = new Notification(config, error);
                boolean sent = notif.deliver();
                if(!sent) {
                    // TODO: Write error to disk for later sending
                }
            }
        }).start();

        // Flush any old errors
        flush();
    }

    public void setContext(Activity context) {
        setContext(getActivityName(context));
    }

    private void flush() {
        new Thread(new Runnable() {
            public void run() {
                // Notification notif = new Notification(config);

                // TODO: Load all error files, add to notif
                
                // boolean sent = notif.deliver();
                // if(!sent) {
                //     // TODO: Write error to disk for later sending
                // }

                // TODO: Delete files if we sent ok
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

    public void addActivity(Activity activity) {
        System.out.println("addActivity: " + getActivityName(activity));
        pruneActivityStack();
        activityStack.add(new WeakReference<Context>(activity));
    }
    
    private String getActivityName(Object obj) {
        String name = obj.getClass().getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    private void pruneActivityStack() {
        List<WeakReference<Context>> toRemove = new LinkedList<WeakReference<Context>>();
        for(WeakReference<Context> ref : activityStack){
            if(ref.get() == null){
                toRemove.add(ref);
            }
        }
        
        for(WeakReference<Context> ref : toRemove) {
            activityStack.remove(ref);
        }
    }
    
    private List<String> generateActivityStack() {
        System.out.println("generateActivityStack");
        pruneActivityStack();
        
        List<String> goodContexts = new LinkedList<String>();
        for(WeakReference<Context> ref : activityStack){
            if(ref.get() != null){
                System.out.println("\t" + getActivityName(ref.get()));
                goodContexts.add(getActivityName(ref.get()));
            }
        }
        return goodContexts;
    }
}