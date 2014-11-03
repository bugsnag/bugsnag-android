package com.bugsnag.android;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.SystemClock;

/**
 * This class contains information about the current app which changes
 * over time, including memory usage.
 */
class AppState implements JsonStreamer.Streamable {
    private Configuration config;
    private Context appContext;
    private static Long startTime;

    public AppState(Configuration config, Context appContext) {
        this.config = config;
        this.appContext = appContext;
        this.startTime = SystemClock.elapsedRealtime();
    }

    public void toStream(JsonStreamer writer) {
        writer.beginObject()
            .name("duration").value(SystemClock.elapsedRealtime() - startTime)
            .name("durationInForeground").value("TODO: Requires activity instrumentation")
            .name("inForeground").value("TODO: Requires activity instrumentation")
            .name("screenStack").value("TODO: Requires activity instrumentation")
            .name("activeScreen").value("TODO: Requires activity instrumentation")
            .name("memoryUsage").value(memoryUsage.get())
            .name("lowMemory").value(lowMemory.get())
        .endObject();
    }

    /**
     * Get the actual memory used by the VM (which may not be the total used
     * by the app in the case of NDK usage).
     */
    private SafeValue<Long> memoryUsage = new SafeValue<Long>() {
        @Override
        public Long calc() {
            return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        }
    };

    /**
     * Check if the device is currently running low on memory.
     */
    private SafeValue<Boolean> lowMemory = new SafeValue<Boolean>() {
        @Override
        public Boolean calc() {
            ActivityManager activityManager = (ActivityManager)appContext.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);

            return memInfo.lowMemory;
        }
    };
}
