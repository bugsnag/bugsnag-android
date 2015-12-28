package com.bugsnag.android;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Information about the current Android device which doesn't change over time,
 * including screen and locale information.
 * <p/>
 * App information in this class is cached during construction for faster
 * subsequent lookups and to reduce GC overhead.
 */
class DeviceData implements JsonStream.Streamable {
    private Context appContext;

    private Float screenDensity;
    private Integer dpi;
    private String screenResolution;
    private long totalMemory;
    private Boolean rooted;
    private String locale;
    private String id;

    DeviceData(Context appContext) {
        this.appContext = appContext;

        Resources resources = appContext.getResources();
        screenDensity = getScreenDensity(resources);
        dpi = getScreenDensityDpi(resources);
        screenResolution = getScreenResolution(resources);
        totalMemory = getTotalMemory();
        rooted = isRooted();
        locale = getLocale();
        id = getAndroidId();
    }

    public void toStream(JsonStream writer) throws IOException {
        writer.beginObject();
            writer.name("osName").value("android");
            writer.name("manufacturer").value(android.os.Build.MANUFACTURER);
            writer.name("brand").value(android.os.Build.BRAND);
            writer.name("model").value(android.os.Build.MODEL);
            writer.name("id").value(id);

            writer.name("apiLevel").value(android.os.Build.VERSION.SDK_INT);
            writer.name("osVersion").value(android.os.Build.VERSION.RELEASE);
            writer.name("osBuild").value(android.os.Build.DISPLAY);

            writer.name("locale").value(locale);

            writer.name("totalMemory").value(totalMemory);

            writer.name("jailbroken").value(rooted);

            writer.name("screenDensity").value(screenDensity);
            writer.name("dpi").value(dpi);
            writer.name("screenResolution").value(screenResolution);
        writer.endObject();
    }

    public String getUserId() {
        return id;
    }

    /**
     * The screen density scaling factor of the current Android device
     */
    private Float getScreenDensity(Resources resources) {
        if (resources == null)
            return null;
        return resources.getDisplayMetrics().density;
    }

    /**
     * The screen density of the current Android device in dpi, eg. 320
     */
    private Integer getScreenDensityDpi(Resources resources) {
        if (resources == null)
            return null;
        return resources.getDisplayMetrics().densityDpi;
    }

    /**
     * The screen resolution of the current Android device in px, eg. 1920x1080
     */
    private String getScreenResolution(Resources resources) {
        if (resources == null)
            return null;
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return String.format("%dx%d", Math.max(metrics.widthPixels, metrics.heightPixels), Math.min(metrics.widthPixels, metrics.heightPixels));
    }

    /**
     * Get the total memory available on the current Android device, in bytes
     */
    private long getTotalMemory() {
        if (Runtime.getRuntime().maxMemory() != Long.MAX_VALUE) {
            return Runtime.getRuntime().maxMemory();
        } else {
            return Runtime.getRuntime().totalMemory();
        }
    }

    private static final String[] ROOT_INDICATORS = new String[]{
        // Common binaries
        "/system/xbin/su",
        "/system/bin/su",
        // < Android 5.0
        "/system/app/Superuser.apk",
        "/system/app/SuperSU.apk",
        // >= Android 5.0
        "/system/app/Superuser",
        "/system/app/SuperSU",
        // Fallback
        "/system/xbin/daemonsu"
    };

    /**
     * Check if the current Android device is rooted
     */
    private Boolean isRooted() {
        if (android.os.Build.TAGS != null && android.os.Build.TAGS.contains("test-keys"))
            return true;

        try {
            for (String candidate : ROOT_INDICATORS) {
                if (new File(candidate).exists())
                    return true;
            }
        } catch (Exception ignore) {
            return null;
        }
        return false;
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
