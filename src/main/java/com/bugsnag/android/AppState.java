package com.bugsnag.android;

import java.util.List;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.SystemClock;

/**
 * This class contains information about the current app which changes
 * over time, including memory usage.
 */
class AppState implements JsonStream.Streamable {
    private Context appContext;
    private static Long startTime;

    AppState(Context appContext) {
        this.appContext = appContext;
        this.startTime = SystemClock.elapsedRealtime();
    }

    public void toStream(JsonStream writer) {
        writer.beginObject()
            .name("duration").value(SystemClock.elapsedRealtime() - startTime)
            .name("inForeground").value(isInForeground())
            .name("activeScreen").value(getActiveScreen())
            .name("memoryUsage").value(getMemoryUsage())
            .name("lowMemory").value(isLowMemory())
        .endObject();
    }

    /**
     * Get the actual memory used by the VM (which may not be the total used
     * by the app in the case of NDK usage).
     */
    private Long getMemoryUsage() {
        try {
            return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        } catch (Exception e) {
            Logger.warn("Could not get memoryUsage");
        }
        return null;
    }

    /**
     * Check if the device is currently running low on memory.
     */
    private Boolean isLowMemory() {
        try {
            ActivityManager activityManager = (ActivityManager)appContext.getSystemService(Context.ACTIVITY_SERVICE);
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
    private String getActiveScreen() {
        try {
            ActivityManager activityManager = (ActivityManager)appContext.getSystemService(Context.ACTIVITY_SERVICE);
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
    private Boolean isInForeground() {
        try {
            ActivityManager activityManager = (ActivityManager)appContext.getSystemService(Context.ACTIVITY_SERVICE);
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
}
