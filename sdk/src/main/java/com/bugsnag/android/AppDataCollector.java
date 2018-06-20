package com.bugsnag.android;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Collects various data on the application state
 */
class AppDataCollector {

    static final String RELEASE_STAGE_DEVELOPMENT = "development";
    static final String RELEASE_STAGE_PRODUCTION = "production";

    private final Client client;
    final String appName;

    AppDataCollector(Client client) {
        this.client = client;
        appName = getAppName(client.appContext);
    }

    // TODO cache values where appropriate

    AppDataSummary generateAppDataSummary() {
        AppDataSummary data = new AppDataSummary();
        populateAppDataSummary(data);
        return data;
    }

    AppData generateAppData() {
        AppData data = new AppData();
        populateAppDataSummary(data);

        String packageName = calculatePackageName(client.appContext);
        data.setPackageName(packageName);

        data.setBuildUuid(client.config.getBuildUUID());
        data.setDuration(getDurationMs());

        long nowMs = System.currentTimeMillis();
        long foregroundMs = client.sessionTracker.getDurationInForegroundMs(nowMs);
        data.setDurationInForeground(foregroundMs);

        data.setInForeground(client.sessionTracker.isInForeground());

        return data;
    }

    // TODO migrate metadata fields to separate class
    void addAppMetaData(MetaData metaData) {
        metaData.addToTab("app", "name", appName);
        metaData.addToTab("app", "packageName", calculatePackageName(client.appContext));
        metaData.addToTab("app", "versionName", calculateVersionName(client.appContext));
        metaData.addToTab("app", "activeScreen", getActiveScreenClass());
        metaData.addToTab("app", "memoryUsage", getMemoryUsage());
        metaData.addToTab("app", "lowMemory", isLowMemory(client.appContext));
    }

    @Nullable
    String getActiveScreenClass() {
        return client.sessionTracker.getContextActivity();
    }

    private void populateAppDataSummary(AppDataSummary data) {
        Configuration config = client.config;

        data.setVersionCode(calculateVersionCode(client.appContext));
        data.setCodeBundleId(config.getCodeBundleId());

        String configType = config.getNotifierType();

        if (configType != null) {
            data.setNotifierType(configType);
        }

        String releaseStage;
        if (config.getReleaseStage() != null) {
            releaseStage = config.getReleaseStage();
        } else {
            releaseStage = guessReleaseStage(client.appContext);
        }
        data.setReleaseStage(releaseStage);

        String versionName;
        if (config.getAppVersion() != null) {
            versionName = config.getAppVersion();
        } else {
            versionName = calculateVersionName(client.appContext);
        }

        data.setVersionName(versionName);
    }




    // TODO refactor below!


    /**
     * The version code of the running Android app, from android:versionCode
     * in AndroidManifest.xml
     */
    @Nullable
    static Integer calculateVersionCode(@NonNull Context appContext) {
        try {
            String packageName = appContext.getPackageName();
            return appContext.getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException exception) {
            Logger.warn("Could not get versionCode");
        }
        return null;
    }

    /**
     * The version code of the running Android app, from android:versionName
     * in AndroidManifest.xml
     */
    @Nullable
    static String calculateVersionName(@NonNull Context appContext) {
        try {
            String packageName = appContext.getPackageName();
            return appContext.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException exception) {
            Logger.warn("Could not get versionName");
        }
        return null;
    }

    /**
     * Guess the release stage of the running Android app by checking the
     * android:debuggable flag from AndroidManifest.xml
     */
    @NonNull
    static String guessReleaseStage(@NonNull Context appContext) {
        try {
            String packageName = appContext.getPackageName();
            PackageManager packageManager = appContext.getPackageManager();
            int appFlags = packageManager.getApplicationInfo(packageName, 0).flags;
            if ((appFlags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                return RELEASE_STAGE_DEVELOPMENT;
            }
        } catch (PackageManager.NameNotFoundException exception) {
            Logger.warn("Could not get releaseStage");
        }
        return RELEASE_STAGE_PRODUCTION;
    }


    /**
     * Get the actual memory used by the VM (which may not be the total used
     * by the app in the case of NDK usage).
     */
    @NonNull
    static Long getMemoryUsage() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /**
     * Check if the device is currently running low on memory.
     */
    @Nullable
    static Boolean isLowMemory(@NonNull Context appContext) {
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

    /**
     * Get the time in milliseconds since Bugsnag was initialized, which is a
     * good approximation for how long the app has been running.
     */
    static long getDurationMs() {
        return SystemClock.elapsedRealtime() - startTimeMs;
    }

    /**
     * The package name of the running Android app, eg: com.example.myapp
     */
    @NonNull
    static String calculatePackageName(@NonNull Context appContext) {
        return appContext.getPackageName();
    }

    private static final long startTimeMs = SystemClock.elapsedRealtime();

    /**
     * The name of the running Android app, from android:label in
     * AndroidManifest.xml
     */
    @Nullable
    static String getAppName(@NonNull Context appContext) {
        try {
            PackageManager packageManager = appContext.getPackageManager();
            String packageName = appContext.getPackageName();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);

            return (String) packageManager.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException exception) {
            Logger.warn("Could not get app name");
        }
        return null;
    }

}
