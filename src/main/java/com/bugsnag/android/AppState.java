package com.bugsnag.android;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.SystemClock;

/**
 * This class contains information about the current app which changes
 * over time, including memory usage.
 */
class AppState implements JsonStream.Streamable {
    private Configuration config;
    private Context appContext;
    private static Long startTime;

    AppState(Configuration config, Context appContext) {
        this.config = config;
        this.appContext = appContext;
        this.startTime = SystemClock.elapsedRealtime();
    }

    public void toStream(JsonStream writer) {
        writer.beginObject()
            .name("duration").value(SystemClock.elapsedRealtime() - startTime)
            .name("durationInForeground").value("TODO: Requires activity instrumentation")
            .name("inForeground").value("TODO: Requires activity instrumentation")
            .name("screenStack").value("TODO: Requires activity instrumentation")
            .name("activeScreen").value("TODO: Requires activity instrumentation")
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
}
