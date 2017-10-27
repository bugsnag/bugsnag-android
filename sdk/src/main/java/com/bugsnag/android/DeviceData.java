package com.bugsnag.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/**
 * Information about the current Android device which doesn't change over time,
 * including screen and locale information.
 * <p/>
 * App information in this class is cached during construction for faster
 * subsequent lookups and to reduce GC overhead.
 */
class DeviceData implements JsonStream.Streamable {

    private static final String INSTALL_ID_KEY = "install.iud";

    @Nullable
    protected final Float screenDensity;
    @Nullable
    protected final Integer dpi;
    @Nullable
    protected final String screenResolution;
    @NonNull
    protected final Long totalMemory;
    @Nullable
    protected final Boolean rooted;
    @NonNull
    protected final String locale;
    @Nullable
    protected String id;
    @NonNull
    protected final String[] cpuAbi;

    DeviceData(@NonNull Context appContext, @NonNull SharedPreferences sharedPref) {
        screenDensity = getScreenDensity(appContext);
        dpi = getScreenDensityDpi(appContext);
        screenResolution = getScreenResolution(appContext);
        totalMemory = getTotalMemory();
        rooted = isRooted();
        locale = getLocale();
        id = retrieveUniqueInstallId(sharedPref);
        cpuAbi = getCpuAbi();
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
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

        writer.name("cpuAbi").beginArray();
        for (String s : cpuAbi) {
            writer.value(s);
        }
        writer.endArray();

        writer.endObject();
    }

    @NonNull
    public String getUserId() {
        return id;
    }

    void setId(@Nullable String id) {
        this.id = id;
    }

    /**
     * The screen density scaling factor of the current Android device
     */
    @Nullable
    private static Float getScreenDensity(@NonNull Context appContext) {
        Resources resources = appContext.getResources();
        if (resources == null)
            return null;
        return resources.getDisplayMetrics().density;
    }

    /**
     * The screen density of the current Android device in dpi, eg. 320
     */
    @Nullable
    private static Integer getScreenDensityDpi(@NonNull Context appContext) {
        Resources resources = appContext.getResources();
        if (resources == null)
            return null;
        return resources.getDisplayMetrics().densityDpi;
    }

    /**
     * The screen resolution of the current Android device in px, eg. 1920x1080
     */
    @Nullable
    private static String getScreenResolution(@NonNull Context appContext) {
        Resources resources = appContext.getResources();
        if (resources == null)
            return null;
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return String.format(Locale.US, "%dx%d", Math.max(metrics.widthPixels, metrics.heightPixels), Math.min(metrics.widthPixels, metrics.heightPixels));
    }

    /**
     * Get the total memory available on the current Android device, in bytes
     */
    @NonNull
    private static Long getTotalMemory() {
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
        "/system/xbin/daemonsu",
        // Systemless root
        "/su/bin"
    };

    /**
     * Check if the current Android device is rooted
     */
    @Nullable
    private static Boolean isRooted() {
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
    @NonNull
    private static String getLocale() {
        return Locale.getDefault().toString();
    }

    /**
     * Get the unique id for the current app installation, creating a unique UUID if needed
     */
    @Nullable
    private String retrieveUniqueInstallId(@NonNull SharedPreferences sharedPref) {
        String installId = sharedPref.getString(INSTALL_ID_KEY, null);

        if (installId == null) {
            installId = UUID.randomUUID().toString();
            sharedPref.edit().putString(INSTALL_ID_KEY, installId).commit();
        }
        return installId;
    }

    /**
     * Gets information about the CPU / API
     */
    @NonNull
    private static String[] getCpuAbi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return SupportedAbiWrapper.getSupportedAbis();
        }
        return Abi2Wrapper.getAbi1andAbi2();
    }

    /**
     * Wrapper class to allow the test framework to use the correct version of the CPU / ABI
     */
    private static class SupportedAbiWrapper {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        public static String[] getSupportedAbis() {
            return Build.SUPPORTED_ABIS;
        }
    }

    /**
     * Wrapper class to allow the test framework to use the correct version of the CPU / ABI
     */
    private static class Abi2Wrapper {
        @NonNull
        public static String[] getAbi1andAbi2() {
            return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        }
    }
}
