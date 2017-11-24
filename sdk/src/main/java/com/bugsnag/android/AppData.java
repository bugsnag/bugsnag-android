package com.bugsnag.android;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Information about the running Android app which doesn't change over time,
 * including app name, version and release stage.
 * <p/>
 * App information in this class is cached during construction for faster
 * subsequent lookups and to reduce GC overhead.
 */
class AppData extends AppDataSummary {

    private static final long startTime = SystemClock.elapsedRealtime();

    @Nullable
    final String appName;

    @NonNull
    private final Context appContext;
    private final SessionTracker sessionTracker;

    @NonNull
    protected final String packageName;

    AppData(@NonNull Context appContext, @NonNull Configuration config, SessionTracker sessionTracker) {
        super(appContext, config);
        this.appContext = appContext;
        this.sessionTracker = sessionTracker;
        appName = getAppName(appContext);
        packageName = getPackageName(appContext);
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        serialiseMinimalAppData(writer);

        writer.name("id").value(packageName);
        writer.name("buildUUID").value(config.getBuildUUID());
        writer.name("duration").value(getDuration());
        writer.name("durationInForeground").value(sessionTracker.getDurationInForeground(System.currentTimeMillis()));
        writer.name("inForeground").value(sessionTracker.isInForeground());

        // TODO migrate legacy fields
        writer.name("name").value(appName);
        writer.name("packageName").value(packageName);
        writer.name("versionName").value(versionName);
        writer.name("activeScreen").value(getActiveScreenClass());
        writer.name("memoryUsage").value(getMemoryUsage());
        writer.name("lowMemory").value(isLowMemory(appContext));
        writer.endObject();
    }

    /**
     * The name of the running Android app, from android:label in
     * AndroidManifest.xml
     */
    @Nullable
    private static String getAppName(@NonNull Context appContext) {
        try {
            PackageManager packageManager = appContext.getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(appContext.getPackageName(), 0);

            return (String) packageManager.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.warn("Could not get app name");
        }
        return null;
    }

    @Nullable
    String getActiveScreenClass() {
        try {
            ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
            ActivityManager.RunningTaskInfo runningTask = tasks.get(0);
            return runningTask.topActivity.getClassName();
        } catch (Exception e) {
            Logger.warn("Could not get active screen information, we recommend granting the 'android.permission.GET_TASKS' permission");
        }
        return null;
    }

    /**
     * Get the actual memory used by the VM (which may not be the total used
     * by the app in the case of NDK usage).
     */
    @NonNull
    private static Long getMemoryUsage() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /**
     * Check if the device is currently running low on memory.
     */
    @Nullable
    private static Boolean isLowMemory(@NonNull Context appContext) {
        try {
            ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);

            return memInfo.lowMemory;
        } catch (Exception e) {
            Logger.warn("Could not check lowMemory status");
        }
        return null;
    }

    /**
     * Get the time in milliseconds since Bugsnag was initialized, which is a
     * good approximation for how long the app has been running.
     */
    private static long getDuration() {
        return SystemClock.elapsedRealtime() - startTime;
    }


    /**
     * The package name of the running Android app, eg: com.example.myapp
     */
    @NonNull
    private static String getPackageName(@NonNull Context appContext) {
        return appContext.getPackageName();
    }
}
