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
