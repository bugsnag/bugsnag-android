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
    private static String uuid;
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
        config.getAppVersion().set(getPackageVersionName(packageName));
        config.getReleaseStage().set(guessReleaseStage(packageName));

        this.initialiseDeviceData();
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

    public JSONObject getDeviceState() {
        JSONObject deviceState = super.getDeviceState();

        JSONUtils.safePutOpt(deviceState, "freeMemory", totalFreeMemory());
        JSONUtils.safePutOpt(deviceState, "orientation", getOrientation());
        JSONUtils.safePutOpt(deviceState, "batteryLevel", getChargeLevel());
        JSONUtils.safePutOpt(deviceState, "freeDisk", getFreeDiskSpace());
        JSONUtils.safePutOpt(deviceState, "charging", getCharging());
        JSONUtils.safePutOpt(deviceState, "locationStatus", getGpsAllowed());
        JSONUtils.safePutOpt(deviceState, "networkAccess", getNetworkStatus());

        return deviceState;
    }

    public JSONObject getDeviceData() {
        super.getDeviceData();

        JSONUtils.safePutOpt(deviceData, "id", getUUID());

        return deviceData;
    }

    public String getContext() {
        return config.getContext().get(ActivityStack.getTopActivityName());
    }

    public JSONObject getUser() {
        JSONObject user = super.getUser();

        if(user.optString("id").equals("")) {
            JSONUtils.safePut(user, "id", getUUID());
        }

        return user;
    }

    //
    //
    // Private functions
    //
    //

    protected void initialiseDeviceData() {
        // osVersion is done by parent class

        JSONUtils.safePutOpt(deviceData, "manufacturer", android.os.Build.MANUFACTURER);
        JSONUtils.safePutOpt(deviceData, "model", android.os.Build.MODEL);
        JSONUtils.safePutOpt(deviceData, "screenDensity", applicationContext.getResources().getDisplayMetrics().density);
        JSONUtils.safePutOpt(deviceData, "screenResolution", getResolution());
        JSONUtils.safePutOpt(deviceData, "totalMemory", totalMemoryAvailable());
        JSONUtils.safePutOpt(deviceData, "osName", "android");
        JSONUtils.safePutOpt(deviceData, "osBuild", android.os.Build.DISPLAY);
        JSONUtils.safePutOpt(deviceData, "apiLevel", android.os.Build.VERSION.SDK_INT);
        JSONUtils.safePutOpt(deviceData, "jailbroken", checkIsRooted());
        JSONUtils.safePutOpt(deviceData, "locale", Locale.getDefault().toString());
    }

    protected void initialiseAppData() {
        // Release stage and version added by parent class

        JSONUtils.safePutOpt(appData, "id", packageName);
        JSONUtils.safePutOpt(appData, "packageName", packageName);
        JSONUtils.safePutOpt(appData, "name", getAppName());
        JSONUtils.safePutOpt(appData, "versionName", getPackageVersionName(packageName));
        JSONUtils.safePutOpt(appData, "versionCode", getPackageVersionCode(packageName));
    }

    protected static void startSessionTimer() {
        if(startTime == null) {
            startTime = SystemClock.elapsedRealtime();
        }
    }

    // We return the lowest disk space out of the internal and sd card storage
    protected Long getFreeDiskSpace() {
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

    protected Boolean getCharging() {
        Boolean isCharging = null;
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = applicationContext.registerReceiver(null, ifilter);

            int status = batteryStatus.getIntExtra("status", -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL;
        } catch (Exception e) {
            config.logger.warn(e);
        }

        return isCharging;
    }

    protected Float getChargeLevel() {
        Float chargeLevel = null;
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = applicationContext.registerReceiver(null, ifilter);

            int level = batteryStatus.getIntExtra("level", -1);
            int scale = batteryStatus.getIntExtra("scale", -1);

            chargeLevel = level / (float)scale;
        } catch (Exception e) {
            config.logger.warn(e);
        }

        return chargeLevel;
    }

    protected String getOrientation() {
        String orientation = null;

        try {
            switch(applicationContext.getResources().getConfiguration().orientation) {
                case android.content.res.Configuration.ORIENTATION_LANDSCAPE:
                    orientation = "landscape";
                    break;
                case android.content.res.Configuration.ORIENTATION_PORTRAIT:
                    orientation = "portrait";
                    break;
            }
        } catch(Exception e) {
            config.logger.warn(e);
        }

        return orientation;
    }

    protected String getAppName() {
        String appName = null;
        try {
            appName = applicationContext.getPackageManager().getApplicationInfo(packageName, 0).name;
        } catch(Exception e) {
            config.logger.warn(e);
        }
        return appName;
    }

    protected String getResolution() {
        String resolution = null;

        try {
            DisplayMetrics metrics = applicationContext.getResources().getDisplayMetrics();
            resolution = String.format("%dx%d", Math.max(metrics.widthPixels, metrics.heightPixels), Math.min(metrics.widthPixels, metrics.heightPixels));
        } catch(Exception e) {
            config.logger.warn(e);
        }

        return resolution;
    }

    protected int getPackageVersionCode(String packageName) {
        int versionCode = 0;

        try {
            PackageInfo pi = applicationContext.getPackageManager().getPackageInfo(packageName, 0);
            versionCode = pi.versionCode;
        } catch(Exception e) {
            config.logger.warn("Could not get package versionCode", e);
        }

        return versionCode;
    }

    protected String getPackageVersionName(String packageName) {
        String packageVersion = null;

        try {
            PackageInfo pi = applicationContext.getPackageManager().getPackageInfo(packageName, 0);
            packageVersion = pi.versionName;
        } catch(Exception e) {
            config.logger.warn("Could not get package versionName", e);
        }

        return packageVersion;
    }

    protected String guessReleaseStage(String packageName) {
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

    protected synchronized String getUUID() {
        if(uuid != null) return uuid;

        final SharedPreferences settings = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        uuid = settings.getString("userId", null);
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

    // This returns the maximum memory the VM can allocate which != the total
    // memory on the phone.
    protected Long totalMemoryAvailable() {
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

    // This is the amount of memory remaining that the VM can allocate.
    protected Long totalFreeMemory() {
        Long freeMemory = null;

        try {
            freeMemory = totalMemoryAvailable() - memoryUsedByApp();
        } catch(Exception e) {
            config.logger.warn(e);
        }

        return freeMemory;
    }

    // This is the actual memory used by the VM (which may not be the total used
    // by the app in the case of NDK usage).
    protected Long memoryUsedByApp() {
        Long memoryUsedByApp = null;

        try {
            memoryUsedByApp = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        } catch(Exception e) {
            config.logger.warn(e);
        }

        return memoryUsedByApp;
    }

    protected Boolean lowMemoryState() {
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

    // We might be able to improve this by checking for su, but i have seen
    // some reports that su is on non rooted phones too
    protected boolean checkIsRooted() {
        return checkTestKeysBuild() || checkSuperUserAPK();
    }

    protected boolean checkTestKeysBuild() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    protected boolean checkSuperUserAPK() {
        try {
            File file = new File("/system/app/Superuser.apk");
            return file.exists();
        } catch (Exception e) {
            return false;
        }
    }

    // Requires android.permission.ACCESS_NETWORK_STATE
    protected String getNetworkStatus() {
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
                    // We default to cellular as the other enums are all cellular in some
                    // form or another
                    networkStatus = "cellular";
                }
            } else {
                networkStatus = "none";
            }
        } catch(Exception e) {
            config.logger.warn(e);
        }

        return networkStatus;
    }

    protected String getGpsAllowed() {
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
