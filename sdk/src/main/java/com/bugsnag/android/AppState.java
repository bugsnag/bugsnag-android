package com.bugsnag.android;

import android.app.ActivityManager;
import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Information about the running Android app which can change over time,
 * including memory usage and active screen information.
 * <p/>
 * App information in this class is not cached, and is recalculated every
 * time toStream is called.
 */
class AppState implements JsonStream.Streamable {
    private static final Long startTime = SystemClock.elapsedRealtime();

    @NonNull
    private final Long duration;
    @Nullable
    private final Boolean inForeground;
    @Nullable
    private final String activeScreen;
    @NonNull
    private final Long memoryUsage;
    @Nullable
    private final Boolean lowMemory;

    AppState(@NonNull Context appContext) {
        duration = getDuration();
        inForeground = isInForeground(appContext);
        activeScreen = getActiveScreen(appContext);
        memoryUsage = getMemoryUsage();
        lowMemory = isLowMemory(appContext);
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();

        writer.name("duration").value(duration);
        writer.name("inForeground").value(inForeground);
        writer.name("activeScreen").value(activeScreen);
        writer.name("memoryUsage").value(memoryUsage);
        writer.name("lowMemory").value(lowMemory);

        writer.endObject();
    }

    @Nullable
    public String getActiveScreenClass() {
        if (activeScreen != null) {
            return activeScreen.substring(activeScreen.lastIndexOf('.') + 1);
        } else {
            return null;
        }
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
     * Get the name of the top-most activity. Requires the GET_TASKS permission,
     * which defaults to true in Android 5.0+.
     */
    @Nullable
    private static String getActiveScreen(@NonNull Context appContext) {
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
     * Get the name of the top-most activity. Requires the GET_TASKS permission,
     * which defaults to true in Android 5.0+.
     */
    @Nullable
    private static Boolean isInForeground(@NonNull Context appContext) {
        try {
            ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
            if (tasks.isEmpty()) {
                return false;
            }

            ActivityManager.RunningTaskInfo runningTask = tasks.get(0);
            return runningTask.topActivity.getPackageName().equalsIgnoreCase(appContext.getPackageName());
        } catch (Exception e) {
            Logger.warn("Could not check if app is in the foreground, we recommend granting the 'android.permission.GET_TASKS' permission");
        }

        return null;
    }

    /**
     * Get the time in milliseconds since Bugsnag was initialized, which is a
     * good approximation for how long the app has been running.
     */
    @NonNull
    private static Long getDuration() {
        return SystemClock.elapsedRealtime() - startTime;
    }
}
