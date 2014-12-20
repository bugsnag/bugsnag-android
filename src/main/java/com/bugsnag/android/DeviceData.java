package com.bugsnag.android;

import java.io.File;
import java.util.Locale;

import android.content.Context;
import android.content.ContentResolver;
import android.provider.Settings;
import android.util.DisplayMetrics;

/**
 * Information about the current Android device which doesn't change over time,
 * including screen and locale information.
 *
 * App information in this class is cached during construction for faster
 * subsequent lookups and to reduce GC overhead.
 */
class DeviceData implements JsonStream.Streamable {
    private Context appContext;

    private float screenDensity;
    private float dpi;
    private String screenResolution;
    private long totalMemory;
    private boolean rooted;
    private String locale;
    private String id;

    DeviceData(Context appContext) {
        this.appContext = appContext;

        screenDensity = getScreenDensity();
        dpi = getScreenDensityDpi();
        screenResolution = getScreenResolution();
        totalMemory = getTotalMemory();
        rooted = isRooted();
        locale = getLocale();
        id = getAndroidId();
    }

    public void toStream(JsonStream writer) {
        writer.beginObject();
            writer.name("manufacturer").value(android.os.Build.MANUFACTURER);
            writer.name("brand").value(android.os.Build.BRAND);
            writer.name("model").value(android.os.Build.MODEL);
            writer.name("screenDensity").value(screenDensity);
            writer.name("dpi").value(dpi);
            writer.name("screenResolution").value(screenResolution);
            writer.name("totalMemory").value(totalMemory);
            writer.name("osName").value("android");
            writer.name("osBuild").value(android.os.Build.DISPLAY);
            writer.name("apiLevel").value(android.os.Build.VERSION.SDK_INT);
            writer.name("jailbroken").value(rooted);
            writer.name("locale").value(locale);
            writer.name("osVersion").value(android.os.Build.VERSION.RELEASE);
            writer.name("id").value(id);
        writer.endObject();
    }

    public String getUserId() {
        return id;
    }

    /**
     * The screen density scaling factor of the current Android device
     */
    private float getScreenDensity() {
        return appContext.getResources().getDisplayMetrics().density;
    }

    /**
     * The screen density of the current Android device in dpi, eg. 320
     */
    private float getScreenDensityDpi() {
        return appContext.getResources().getDisplayMetrics().densityDpi;
    }

    /**
     * The screen resolution of the current Android device in px, eg. 1920x1080
     */
    private String getScreenResolution() {
        DisplayMetrics metrics = appContext.getResources().getDisplayMetrics();
        return String.format("%dx%d", Math.max(metrics.widthPixels, metrics.heightPixels), Math.min(metrics.widthPixels, metrics.heightPixels));
    }

    /**
     * Get the total memory available on the current Android device, in bytes
     */
    private long getTotalMemory() {
        if(Runtime.getRuntime().maxMemory() != Long.MAX_VALUE) {
            return Runtime.getRuntime().maxMemory();
        } else {
            return Runtime.getRuntime().totalMemory();
        }
    }

    /**
     * Check if the current Android device is rooted
     */
    private boolean isRooted() {
        boolean hasTestKeys = android.os.Build.TAGS != null && android.os.Build.TAGS.contains("test-keys");
        boolean hasSuperUserApk = false;
        try {
            File file = new File("/system/app/Superuser.apk");
            hasSuperUserApk = file.exists();
        } catch (Exception e) { }

        return hasTestKeys || hasSuperUserApk;
    }

    /**
     * Get the locale of the current Android device, eg en_US
     */
    private String getLocale() {
        return Locale.getDefault().toString();
    }

    /**
     * Get the unique device id for the current Android device
     */
    private String getAndroidId() {
        ContentResolver cr = appContext.getContentResolver();
        return Settings.Secure.getString(cr, Settings.Secure.ANDROID_ID);
    }
}
