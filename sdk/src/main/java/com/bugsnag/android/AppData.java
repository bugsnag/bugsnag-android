package com.bugsnag.android;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * Information about the running Android app, including app name, version and release stage.
 */
public class AppData extends AppDataSummary {

    private static final long startTimeMs = SystemClock.elapsedRealtime();

    @Nullable
    final String appName;

    @NonNull
    private final Context appContext;
    private final SessionTracker sessionTracker;

    @NonNull
    private String packageName;

    @Nullable
    private String buildUuid;

    private long durationMs;
    private long foregroundMs;
    private boolean inForeground;

    AppData(@NonNull Context appContext,
            @NonNull Configuration config,
            SessionTracker sessionTracker) {
        super(appContext, config);
        this.appContext = appContext;
        this.sessionTracker = sessionTracker;
        appName = getAppName(appContext);

        packageName = calculatePackageName(appContext);
        buildUuid = config.getBuildUUID();
        durationMs = getDurationMs();
        foregroundMs = sessionTracker.getDurationInForegroundMs(System.currentTimeMillis());
        inForeground = sessionTracker.isInForeground();
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        serialiseMinimalAppData(writer);
        writer.name("id").value(packageName);
        writer.name("buildUUID").value(buildUuid);
        writer.name("duration").value(durationMs);
        writer.name("durationInForeground").value(foregroundMs);
        writer.name("inForeground").value(inForeground);
        writer.endObject();
    }

    // TODO migrate metadata fields to separate class
    void addAppMetaData(MetaData metaData) {
        metaData.addToTab("app", "name", appName);
        metaData.addToTab("app", "packageName", packageName);
        metaData.addToTab("app", "versionName", getVersionName());
        metaData.addToTab("app", "activeScreen", getActiveScreenClass());
        metaData.addToTab("app", "memoryUsage", getMemoryUsage());
        metaData.addToTab("app", "lowMemory", isLowMemory(appContext));
    }

    /**
     * The name of the running Android app, from android:label in
     * AndroidManifest.xml
     */
    @Nullable
    private static String getAppName(@NonNull Context appContext) {
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

    /**
     * @return the application's package name
     */
    @NonNull
    public String getPackageName() {
        return packageName;
    }

    /**
     * Overrides the application's default package name
     *
     * @param packageName the package name
     */
    public void setPackageName(@NonNull String packageName) {
        this.packageName = packageName;
    }

    /**
     * @return the application's bugsnag build UUID
     */
    @Nullable
    public String getBuildUuid() {
        return buildUuid;
    }

    /**
     * Overrides the application's default bugsnag build UUID
     *
     * @param buildUuid the bugsnag build UUID
     */
    public void setBuildUuid(@Nullable String buildUuid) {
        this.buildUuid = buildUuid;
    }

    /**
     * @return the duration in ms for which the app has been running
     */
    public long getDuration() {
        return durationMs;
    }

    /**
     * Overrides the duration in ms for which the app has been running
     *
     * @param durationMs the new duration in ms
     */
    public void setDuration(long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * @return the duration in ms for which the app has been running in the foreground
     */
    public long getDurationInForeground() {
        return foregroundMs;
    }

    /**
     * Overrides the duration in ms for which the app has been running in the foreground
     *
     * @param foregroundMs the new duration in ms
     */
    public void setDurationInForeground(long foregroundMs) {
        this.foregroundMs = foregroundMs;
    }

    /**
     * @return whether the app is in the foreground or not
     */
    public boolean isInForeground() {
        return inForeground;
    }

    /**
     * Overrides whether the app is in the foreground or not
     *
     * @param inForeground whether the app is in the foreground
     */
    public void setInForeground(boolean inForeground) {
        this.inForeground = inForeground;
    }

    @Nullable
    String getActiveScreenClass() {
        return sessionTracker.getContextActivity();
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
    private static String calculatePackageName(@NonNull Context appContext) {
        return appContext.getPackageName();
    }
}
