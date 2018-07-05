package com.bugsnag.android;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Collects various data on the application state
 */
class AppData {

    private static final long startTimeMs = SystemClock.elapsedRealtime();

    static final String RELEASE_STAGE_DEVELOPMENT = "development";
    static final String RELEASE_STAGE_PRODUCTION = "production";

    private final Client client;
    private final Context appContext;

    private final String packageName;

    @Nullable
    final String appName;

    @Nullable
    private PackageInfo packageInfo;

    @Nullable
    private ApplicationInfo applicationInfo;

    @Nullable
    private PackageManager packageManager;

    AppData(Client client) {
        this.client = client;
        this.appContext = client.appContext;

        // cache values which are widely used, expensive to lookup, or unlikely to change
        packageName = appContext.getPackageName();

        try {
            packageManager = appContext.getPackageManager();
            packageInfo = packageManager.getPackageInfo(packageName, 0);
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException exception) {
            Logger.warn("Could not retrieve package/application information for " + packageName);
        }

        appName = getAppName();
    }

    Map<String, Object> getAppDataSummary() {
        Map<String, Object> map = new HashMap<>();
        Configuration config = client.config;
        map.put("type", calculateNotifierType(config));
        map.put("releaseStage", guessReleaseStage());
        map.put("version", calculateVersionName());
        map.put("versionCode", calculateVersionCode());
        map.put("codeBundleId", config.getCodeBundleId());
        return map;
    }

    Map<String, Object> getAppData() {
        Map<String, Object> map = getAppDataSummary();
        map.put("id", packageName);
        map.put("buildUUID", client.config.getBuildUUID());
        map.put("duration", getDurationMs());
        map.put("durationInForeground", calculateDurationInForeground());
        map.put("inForeground", client.sessionTracker.isInForeground());
        map.put("packageName", packageName);
        return map;
    }

    Map<String, Object> getAppDataMetaData() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", appName);
        map.put("packageName", packageName);
        map.put("versionName", calculateVersionName());
        map.put("activeScreen", getActiveScreenClass());
        map.put("memoryUsage", getMemoryUsage());
        map.put("lowMemory", isLowMemory());
        return map;
    }

    /**
     * Get the time in milliseconds since Bugsnag was initialized, which is a
     * good approximation for how long the app has been running.
     */
    static long getDurationMs() {
        return SystemClock.elapsedRealtime() - startTimeMs;
    }

    /**
     * Calculates the duration the app has been in the foreground
     *
     * @return the duration in ms
     */
    private long calculateDurationInForeground() {
        long nowMs = System.currentTimeMillis();
        return client.sessionTracker.getDurationInForegroundMs(nowMs);
    }

    private String getActiveScreenClass() {
        return client.sessionTracker.getContextActivity();
    }

    @NonNull
    private String calculateNotifierType(Configuration config) {
        String notifierType = config.getNotifierType();

        if (notifierType != null) {
            return notifierType;
        } else {
            return "android";
        }
    }

    /**
     * The version code of the running Android app, from android:versionCode
     * in AndroidManifest.xml
     */
    @Nullable
    private Integer calculateVersionCode() {
        if (packageInfo != null) {
            return packageInfo.versionCode;
        } else {
            return null;
        }
    }

    /**
     * The version code of the running Android app, from android:versionName
     * in AndroidManifest.xml
     */
    @Nullable
    private String calculateVersionName() {
        String configAppVersion = client.config.getAppVersion();

        if (configAppVersion != null) {
            return configAppVersion;
        } else if (packageInfo != null) {
            return packageInfo.versionName;
        } else {
            return null;
        }
    }

    /**
     * Guess the release stage of the running Android app by checking the
     * android:debuggable flag from AndroidManifest.xml. If the release stage was set in
     * {@link Configuration}, this value will be returned instead.
     */
    @NonNull
    String guessReleaseStage() {
        String configStage = client.config.getReleaseStage();

        if (configStage != null) {
            return configStage;
        }
        if (applicationInfo != null) {
            if ((applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                return RELEASE_STAGE_DEVELOPMENT;
            }
        }
        return RELEASE_STAGE_PRODUCTION;
    }

    /**
     * The name of the running Android app, from android:label in
     * AndroidManifest.xml
     */
    @Nullable
    private String getAppName() {
        if (packageManager != null && applicationInfo != null) {
            return packageManager.getApplicationLabel(applicationInfo).toString();
        } else {
            return null;
        }
    }

    /**
     * Get the actual memory used by the VM (which may not be the total used
     * by the app in the case of NDK usage).
     */
    private long getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Check if the device is currently running low on memory.
     */
    @Nullable
    private Boolean isLowMemory() {
        try {
            ActivityManager activityManager =
                (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);

            if (activityManager != null) {
                ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memInfo);
                return memInfo.lowMemory;
            }
        } catch (Exception exception) {
            Logger.warn("Could not check lowMemory status");
        }
        return null;
    }

}
