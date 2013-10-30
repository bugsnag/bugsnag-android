package com.bugsnag.android;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.json.JSONObject;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;

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
        config.getOsVersion().set(android.os.Build.VERSION.RELEASE);
        config.getAppVersion().set(getPackageVersion(packageName));
        config.getReleaseStage().set(guessReleaseStage(packageName));

        this.initialiseHostData();
        this.initialiseAppData();
    }

    public JSONObject getAppState() {
        JSONObject appState = super.getAppState();

        JSONUtils.safePutOpt(appState, "duration", SystemClock.elapsedRealtime() - startTime);
        JSONUtils.safePutOpt(appState, "durationInForeground", ActivityStack.sessionLength());
        JSONUtils.safePutOpt(appState, "lowMemory", lowMemoryState());
        JSONUtils.safePutOpt(appState, "inForeground", ActivityStack.inForeground());
        List<String> activityStackNames = ActivityStack.getNames();
        if(activityStackNames.size() > 0) {
            JSONUtils.safePutOpt(appState, "screenStack", activityStackNames);
        }
        JSONUtils.safePutOpt(appState, "activeScreen", ActivityStack.getTopActivityName());
        JSONUtils.safePutOpt(appState, "memoryUsage", memoryUsedByApp());

        return appState;
    }

    public JSONObject getHostState() {
        JSONObject hostState = super.getHostState();

        JSONUtils.safePutOpt(hostData, "freeMemory", totalFreeMemory());
        JSONUtils.safePutOpt(hostData, "orientation", getOrientation());
        JSONUtils.safePutOpt(hostData, "batteryLevel", getChargeLevel());
        JSONUtils.safePutOpt(hostData, "charging", getCharging());
        JSONUtils.safePutOpt(hostState, "locationStatus", getGpsAllowed());
        JSONUtils.safePutOpt(hostState, "networkAccess", getNetworkStatus());
        
        return hostState;
    }

    public String getContext() {
        return config.getContext().get(ActivityStack.getTopActivityName());
    }

    //
    //
    // Private functions
    //
    //

    private void initialiseHostData() {
        // osVersion is done by parent class

        JSONUtils.safePutOpt(hostData, "id", getUUID());
        JSONUtils.safePutOpt(hostData, "manufacturer", android.os.Build.MANUFACTURER);
        JSONUtils.safePutOpt(hostData, "model", android.os.Build.MODEL);
        JSONUtils.safePutOpt(hostData, "screenDensity", applicationContext.getResources().getDisplayMetrics().density);
        JSONUtils.safePutOpt(hostData, "screenResolution", getResolution());
        JSONUtils.safePutOpt(hostData, "totalMemory", totalMemoryAvailable());
        JSONUtils.safePutOpt(hostData, "freeDisk", getFreeDiskSpace());
        JSONUtils.safePutOpt(hostData, "osName", getOsName());
        JSONUtils.safePutOpt(hostData, "jailbroken", checkIsRooted());
        JSONUtils.safePutOpt(hostData, "locale", Locale.getDefault().toString());
    }

    private void initialiseAppData() {
        // Release stage and version added by parent class

        JSONUtils.safePutOpt(appData, "id", packageName);
        JSONUtils.safePutOpt(appData, "packageName", packageName);
        JSONUtils.safePutOpt(appData, "name", getAppName());
    }

    private static void startSessionTimer() {
        if(startTime == null) {
            startTime = SystemClock.elapsedRealtime();
        }
    }

    private Long getFreeDiskSpace() {
        Long diskSpace = null;

        try {
            StatFs externalStat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long externalBytesAvailable = (long)externalStat.getBlockSize() *(long)externalStat.getBlockCount();

            StatFs internalStat = new StatFs(Environment.getDataDirectory().getPath());
            long internalBytesAvailable = (long)internalStat.getBlockSize() *(long)internalStat.getBlockCount();

            diskSpace = Math.min(internalBytesAvailable, externalBytesAvailable);
        } catch (Exception e) {
            config.logger.warn(e);
        }

        return diskSpace;
    }

    private Boolean getCharging() {
        Boolean isCharging = null;
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = applicationContext.registerReceiver(null, ifilter);

            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL;
        } catch (Exception e) {
            config.logger.warn(e);
        }

        return isCharging;
    }

    private Float getChargeLevel() {
        Float chargeLevel = null;
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = applicationContext.registerReceiver(null, ifilter);

            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            chargeLevel = level / (float)scale;
        } catch (Exception e) {
            config.logger.warn(e);
        }

        return chargeLevel;
    }

    private String getOsName() {
        Field[] fields = android.os.Build.VERSION_CODES.class.getFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            int fieldValue = -1;

            try {
                fieldValue = field.getInt(new Object());
            } catch (Exception e) {
                config.logger.warn(e);
            }

            if (fieldValue == android.os.Build.VERSION.SDK_INT) {
                return fieldName;
            }
        }
        return null;
    }

    private String getOrientation() {
        switch(applicationContext.getResources().getConfiguration().orientation) {
            case android.content.res.Configuration.ORIENTATION_LANDSCAPE:
                return "landscape";
            case android.content.res.Configuration.ORIENTATION_PORTRAIT:
                return "portrait";
            default:
                return null;
        }
    }

    private String getAppName() {
        String appName = null;
        try {
            appName = applicationContext.getPackageManager().getApplicationInfo(packageName, 0).name;
        } catch(Exception e) {
            config.logger.warn(e);
        }
        return appName;
    }

    private String getResolution() {
        DisplayMetrics metrics = applicationContext.getResources().getDisplayMetrics();
        return String.format("%dx%d", Math.max(metrics.widthPixels, metrics.heightPixels), Math.min(metrics.widthPixels, metrics.heightPixels));
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

    private Long totalMemoryAvailable() {
        Long totalMemory = null;

        try {
            if(Runtime.getRuntime().maxMemory() != Long.MAX_VALUE) {
                totalMemory = Runtime.getRuntime().maxMemory();
            } else {
                totalMemory = Runtime.getRuntime().totalMemory();
            }
        } catch(Exception e) {
            config.logger.warn(e);
        }

        return totalMemory;
    }

    private Long totalFreeMemory() {
        Long freeMemory = null;

        try {
            freeMemory = totalMemoryAvailable() - memoryUsedByApp();
        } catch(Exception e) {
            config.logger.warn(e);
        }

        return freeMemory;
    }

    private Long memoryUsedByApp() {
        Long memoryUsedByApp = null;

        try {
            memoryUsedByApp = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        } catch(Exception e) {
            config.logger.warn(e);
        }

        return memoryUsedByApp;
    }

    private Boolean lowMemoryState() {
        Boolean lowMemory = null;
        try {
            ActivityManager activityManager = (ActivityManager)applicationContext.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);

            lowMemory = memInfo.lowMemory;
        } catch(Exception e) {
            config.logger.warn(e);
        }
        return lowMemory;
    }

    private boolean checkIsRooted() {
        return checkTestKeysBuild() || checkSuperUserAPK();
    }

    private boolean checkTestKeysBuild() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    private boolean checkSuperUserAPK() {
        try {
            File file = new File("/system/app/Superuser.apk");
            return file.exists();
        } catch (Exception e) {
            return false;
        }
    }

    private String getNetworkStatus() {
        String networkStatus = null;

        try {
            // Get the network information
            ConnectivityManager cm = (ConnectivityManager)applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if(activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                if(activeNetwork.getType() == 1) {
                    networkStatus = "wifi";
                } else if(activeNetwork.getType() == 9) {
                    networkStatus = "ethernet";
                } else {
                    networkStatus = "cellular";
                }
            } else {
                networkStatus = "none";
            }
        } catch(SecurityException e) {
            // App doesn't have android.permission.ACCESS_NETWORK_STATE permission
        } catch(Exception e) {
            config.logger.warn(e);
        }

        return networkStatus;
    }

    private String getGpsAllowed() {
        String gpsAllowed = null;

        try {
            ContentResolver cr = applicationContext.getContentResolver();
            String providersAllowed = Settings.Secure.getString(cr, Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if(providersAllowed != null && providersAllowed.length() > 0) {
                gpsAllowed = "allowed";
            } else {
                gpsAllowed = "disallowed";
            }
        } catch (Exception e) {
            config.logger.warn(e);
        }

        return gpsAllowed;
    }
}
