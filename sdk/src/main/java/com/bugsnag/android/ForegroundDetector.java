package com.bugsnag.android;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.support.annotation.Nullable;

import java.util.List;

class ForegroundDetector {

    private final ActivityManager activityManager;

    ForegroundDetector(Context context) {
        this.activityManager =
            (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    /**
     * Determines whether or not the application is in the foreground, by using the process'
     * importance as a proxy.
     * <p/>
     * In the unlikely event that information about the process cannot be retrieved, this method
     * will return true. This is deemed preferable as ANRs are only reported when the application
     * is in the foreground, and we would rather deliver false-positives than miss true ANRs in
     * this case. We also need to report 'inForeground' as a boolean value in API calls, and
     * need to keep the definition of the value consistent throughout the application.
     *
     * @return whether the application is in the foreground or not
     */
    boolean isInForeground() {
        ActivityManager.RunningAppProcessInfo info = getProcessInfo();

        if (info != null) {
            return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
        } else { // prefer a potential false positive if process info not available
            return true;
        }
    }

    private ActivityManager.RunningAppProcessInfo getProcessInfo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityManager.RunningAppProcessInfo info =
                new ActivityManager.RunningAppProcessInfo();
            ActivityManager.getMyMemoryState(info);
            return info;
        } else {
            return getProcessInfoPreApi16();
        }
    }

    @Nullable
    private ActivityManager.RunningAppProcessInfo getProcessInfoPreApi16() {
        List<ActivityManager.RunningAppProcessInfo> appProcesses;

        try {
            appProcesses = activityManager.getRunningAppProcesses();
        } catch (SecurityException exc) {
            return null;
        }

        if (appProcesses != null) {
            int pid = Process.myPid();

            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (pid == appProcess.pid) {
                    return appProcess;
                }
            }
        }
        return null;
    }
}
