package com.bugsnag.android;

import java.io.File;
import java.util.Locale;

import android.content.Context;
import android.content.ContentResolver;
import android.provider.Settings;
import android.util.DisplayMetrics;

/**
 * This class contains information about the current device which should
 * not change over time. Device information in this class should be cached
 * for fast subsequent lookups.
 */
class DeviceData implements JsonStream.Streamable {
    private Context appContext;
    private String packageName;

    private Float screenDensity;
    private String screenResolution;
    private Long totalMemory;
    private Boolean rooted;
    private String locale;
    private String id;

    DeviceData(Context appContext) {
        this.appContext = appContext;
        this.packageName = appContext.getPackageName();

        screenDensity = getScreenDensity();
        screenResolution = getScreenResolution();
        totalMemory = getTotalMemory();
        rooted = isRooted();
        locale = getLocale();
        id = getAndroidId();
    }

    public void toStream(JsonStream writer) {
        writer.beginObject()
            .name("manufacturer").value(android.os.Build.MANUFACTURER)
            .name("model").value(android.os.Build.MODEL)
            .name("screenDensity").value(screenDensity)
            .name("screenResolution").value(screenResolution)
            .name("totalMemory").value(totalMemory)
            .name("osName").value("android")
            .name("osBuild").value(android.os.Build.DISPLAY)
            .name("apiLevel").value(android.os.Build.VERSION.SDK_INT)
            .name("jailbroken").value(rooted)
            .name("locale").value(locale)
            .name("osVersion").value(android.os.Build.VERSION.RELEASE)
            .name("id").value(id)
        .endObject();
    }

    private Float getScreenDensity() {
        try {
            return appContext.getResources().getDisplayMetrics().density;
        } catch (Exception e) {
            Logger.warn("Could not get screenDensity");
        }
        return null;
    }

    private String getScreenResolution() {
        try {
            DisplayMetrics metrics = appContext.getResources().getDisplayMetrics();
            return String.format("%dx%d", Math.max(metrics.widthPixels, metrics.heightPixels), Math.min(metrics.widthPixels, metrics.heightPixels));
        } catch (Exception e) {
            Logger.warn("Could not get screenResolution");
        }
        return null;
    }

    private Long getTotalMemory() {
        try {
            Long totalMemory = null;
            if(Runtime.getRuntime().maxMemory() != Long.MAX_VALUE) {
                totalMemory = Runtime.getRuntime().maxMemory();
            } else {
                totalMemory = Runtime.getRuntime().totalMemory();
            }
            return totalMemory;
        } catch (Exception e) {
            Logger.warn("Could not get totalMemory");
        }
        return null;
    }

    private Boolean isRooted() {
        try {
            boolean hasTestKeys = android.os.Build.TAGS != null && android.os.Build.TAGS.contains("test-keys");
            boolean hasSuperUserApk = false;
            try {
                File file = new File("/system/app/Superuser.apk");
                hasSuperUserApk = file.exists();
            } catch (Exception e) { }

            return hasTestKeys || hasSuperUserApk;
        } catch (Exception e) {
            Logger.warn("Could not check if rooted");
        }
        return null;
    }

    private String getLocale() {
        return Locale.getDefault().toString();
    }

    private String getAndroidId() {
        try {
            ContentResolver cr = appContext.getContentResolver();
            return Settings.Secure.getString(cr, Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            Logger.warn("Could not get androidId");
        }
        return null;
    }
}
