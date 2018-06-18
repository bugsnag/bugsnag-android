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
class AppData extends AppDataSummary {

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
            String packageName = appContext.getPackageName();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);

            return (String) packageManager.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException exception) {
            Logger.warn("Could not get app name");
        }
        return null;
    }


    @NonNull
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(@NonNull String packageName) {
        this.packageName = packageName;
    }

    @Nullable
    public String getBuildUuid() {
        return buildUuid;
    }

    public void setBuildUuid(@Nullable String buildUuid) {
        this.buildUuid = buildUuid;
    }

    public long getDuration() {
        return durationMs;
    }

    public void setDuration(long durationMs) {
        this.durationMs = durationMs;
    }

    public long getDurationInForeground() {
        return foregroundMs;
    }

    public void setDurationInForeground(long foregroundMs) {
        this.foregroundMs = foregroundMs;
    }

    public boolean isInForeground() {
        return inForeground;
    }

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
