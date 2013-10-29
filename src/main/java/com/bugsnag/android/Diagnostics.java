package com.bugsnag.android;

import java.util.List;
import java.util.UUID;

import org.json.JSONObject;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.provider.Settings;

import com.bugsnag.Configuration;
import com.bugsnag.utils.JSONUtils;
import com.bugsnag.android.utils.Async;

class Diagnostics extends com.bugsnag.Diagnostics {
    private static final String PREFS_NAME = "Bugsnag";
    private static Long startTime;
    private Context applicationContext;
    private String packageName;

    public Diagnostics(Configuration config, Context context, Client client) {
        super(config);

        applicationContext = context;
        Diagnostics.startSessionTimer();

        packageName = applicationContext.getPackageName();

        // Set up some defaults that people can change in config
        config.setProjectPackages(packageName);
        config.getOsVersion().setComputed(android.os.Build.VERSION.RELEASE);
        config.getAppVersion().setComputed(getPackageVersion(packageName));
        config.getReleaseStage().setComputed(guessReleaseStage(packageName));

        this.initialiseHostData();
        this.initialiseAppData();
    }

    public JSONObject getAppState() {
        JSONObject appState = super.getAppState();

        // Activity stack
        String topActivityName = ActivityStack.getTopActivityName();
        List<String> activityStackNames = ActivityStack.getNames();
        if(activityStackNames.size() > 0) {
            JSONUtils.safePutOpt(appState, "activityStack", activityStackNames);
        }

        // Top activity
        if(topActivityName != null) {
            JSONUtils.safePutOpt(appState, "activity", topActivityName);
        }

        JSONUtils.safePutOpt(appState, "duration", SystemClock.elapsedRealtime() - startTime);

        //TODO:SM inForeground and durationInForeground
        //TODO:SM memoryUsage

        return appState;
    }

    public JSONObject getHostState() {
        JSONObject hostState = super.getHostState();

        // Network status
        String networkStatus = getNetworkStatus();
        if(networkStatus != null) {
            JSONUtils.safePutOpt(hostState, "networkAccess", networkStatus);
        }

        // GPS status
        Boolean gpsEnabled = getGpsEnabled();
        if(gpsEnabled != null) {
            //TODO:SM Format this
            JSONUtils.safePutOpt(hostState, "locationSensor", gpsEnabled);
        }
        
        return hostState;
    }

    public String getContext() {
        return config.getContext().get(ActivityStack.getTopActivityName());
    }

    public JSONObject getMetrics() {
        JSONObject metrics = super.getMetrics();

        JSONUtils.safePutOpt(metrics, "userId", this.getUUID());

        return metrics;
    }

    //
    //
    // Private functions
    //
    //

    private void initialiseHostData() {
        JSONUtils.safePutOpt(hostData, "model", android.os.Build.MODEL);

        // Current memory status
        long totalMemory = totalMemoryOnHost();
        if(totalMemory != 0) {
            JSONUtils.safePutOpt(hostData, "totalMemory", totalMemory);
        }
    }

    private void initialiseAppData() {
        JSONUtils.safePutOpt(appData, "id", packageName);
        JSONUtils.safePutOpt(appData, "packageName", packageName);
        try {
            JSONUtils.safePutOpt(appData, "name", applicationContext.getPackageManager().getApplicationInfo(packageName, 0).name);
        } catch(Exception e) {
            //ignore
            e.printStackTrace();
        }
    }

    private static void startSessionTimer() {
        if(startTime == null) {
            startTime = SystemClock.elapsedRealtime();
        }
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

    // TODO:JS Avoid StrictMode violations caused by getSharedPreferences
    // TODO:JS Avoid StrictMode violations caused by UUID.randomUUID
    private String getUUID() {
        final SharedPreferences settings = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uuid = settings.getString("userId", null);
        if(uuid == null) {
            uuid = UUID.randomUUID().toString();

            // Save if for future
            final String finalUuid = uuid;

            Async.safeAsync(new Runnable() {
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

    private long totalMemoryOnHost() {
        long totalMemory = 0;

        try {
            ActivityManager activityManager = (ActivityManager)applicationContext.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);

            totalMemory = Runtime.getRuntime().maxMemory();
        } catch(Exception e) {
            e.printStackTrace();
        }

        return totalMemory;
    }

    private JSONObject getMemoryInfo() {
        JSONObject memoryInfo = null;

        try {
            ActivityManager activityManager = (ActivityManager)applicationContext.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);

            long totalMem = Runtime.getRuntime().maxMemory();
            long usedMem = Runtime.getRuntime().totalMemory();
            long freeMem = totalMem - usedMem;

            memoryInfo = new JSONObject();
            JSONUtils.safePut(memoryInfo, "Total Available", totalMem);
            JSONUtils.safePut(memoryInfo, "Free", freeMem);
            JSONUtils.safePut(memoryInfo, "Used", usedMem);
            JSONUtils.safePut(memoryInfo, "Low Memory?", memInfo.lowMemory);
        } catch(Exception e) {
            e.printStackTrace();
        }

        return memoryInfo;
    }

    private String getNetworkStatus() {
        String networkStatus = "Unknown";

        // TODO:SM Format this
        try {
            // Get the network information
            ConnectivityManager cm = (ConnectivityManager)applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if(activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                networkStatus = "Connected";
            } else {
                networkStatus = "Not Connected";
            }
        } catch(SecurityException e) {
            // App doesn't have android.permission.ACCESS_NETWORK_STATE permission
        } catch(Exception e) {
            e.printStackTrace();
        }

        return networkStatus;
    }

    private Boolean getGpsEnabled() {
        Boolean gpsEnabled = null;

        try {
            ContentResolver cr = applicationContext.getContentResolver();
            String providersAllowed = Settings.Secure.getString(cr, Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            gpsEnabled = providersAllowed != null && providersAllowed.length() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return gpsEnabled;
    }
}
