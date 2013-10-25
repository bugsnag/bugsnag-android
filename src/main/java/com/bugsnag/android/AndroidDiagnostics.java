package com.bugsnag.android;

import java.util.List;

import org.json.JSONObject;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.provider.Settings;

import com.bugsnag.Diagnostics;
import com.bugsnag.utils.JSONUtils;

class AndroidDiagnostics extends Diagnostics {
    private static Long startTime;
    private Context applicationContext;

    public AndroidDiagnostics(Configuration config, Context context) {
        super(config);

        applicationContext = context;

        AndroidDiagnostics.startSessionTimer();
    }

    public JSONObject getApp() {
        // Adds appVersion and releaseStage
        JSONObject appData = super();
        if (appData == null) appData = new JSONObject();
        
        String packageName = applicationContext.getPackageName();

        JSONUtils.safePutNotNull(appData, "id", packageName);
        JSONUtils.safePutNotNull(appData, "packageName", packageName);
        JSONUtils.safePutNotNull(appData, "name", applicationContext.getPackageManager().getApplicationInfo(packageName, 0).name);

        return appData;
    }

    public JSONObject getAppState() {
        JSONObject appState = super();
        if (appState == null) appState = new JSONObject();

        // Activity stack
        String topActivityName = ActivityStack.getTopActivityName();
        List<String> activityStackNames = ActivityStack.getNames();
        if(activityStackNames.size() > 0) {
            JSONUtils.safePutNotNull(appState, "activityStack", activityStackNames);
        }

        // Top activity
        if(topActivityName != null) {
            JSONUtils.safePutNotNull(appState, "activity", topActivityName);
        }

        JSONUtils.safePutNotNull(appState, "duration", SystemClock.elapsedRealtime() - startTime);

        //TODO:SM inForeground and durationInForeground
        //TODO:SM memoryUsage

        return null;
    }

    public JSONObject getHost() {
        JSONObject hostData = super();
        if (hostData == null) hostData = new JSONObject();

        JSONUtils.safePutNotNull(hostData, "model", android.os.Build.MODEL);

        // Current memory status
        long totalMemory = totalMemoryOnHost();
        if(totalMemory != 0) {
            JSONUtils.safePutNotNull(hostData, "totalMemory", totalMemory);
        }

        return hostData;
    }

    public JSONObject getHostState() {
        JSONObject hostState = super();
        if (hostState == null) hostState = new JSONObject();

        // Network status
        String networkStatus = getNetworkStatus();
        if(networkStatus != null) {
            JSONUtils.safePutNotNull(hostState, "networkAccess", totalMemory);
        }

        // GPS status
        Boolean gpsEnabled = getGpsEnabled();
        if(gpsEnabled != null) {
            //TODO:SM Format this
            JSONUtils.safePutNotNull(hostState, "locationSensor", gpsEnabled);
        }
    }

    public static void startSessionTimer() {
        if(startTime == null) {
            startTime = SystemClock.elapsedRealtime();
        }
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
