package com.bugsnag.android;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Information about the current Android device which doesn't change over time,
 * including screen and locale information.
 * <p/>
 * App information in this class is cached during construction for faster
 * subsequent lookups and to reduce GC overhead.
 */
public class DeviceData extends DeviceDataSummary {

    private static final String INSTALL_ID_KEY = "install.iud";

    private long freeMemory;
    private long totalMemory;

    @Nullable
    private Long freeDisk;

    @Nullable
    private String id;

    @Nullable
    private String orientation;

    @Nullable
    final Float screenDensity;

    @Nullable
    final Integer dpi;

    @Nullable
    final String screenResolution;
    private Context appContext;

    @NonNull
    final String locale;

    @NonNull
    final String[] cpuAbi;

    DeviceData(@NonNull Context appContext, @NonNull SharedPreferences sharedPref) {
        screenDensity = getScreenDensity(appContext);
        dpi = getScreenDensityDpi(appContext);
        screenResolution = getScreenResolution(appContext);
        this.appContext = appContext;
        locale = getLocale();
        id = retrieveUniqueInstallId(sharedPref);
        cpuAbi = getCpuAbi();
        freeMemory = calculateFreeMemory();
        totalMemory = calculateTotalMemory();
        freeDisk = calculateFreeDisk();
        orientation = calculateOrientation(appContext);
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        serialiseMinimalDeviceData(writer);

        writer
            .name("id").value(id)
            .name("freeMemory").value(freeMemory)
            .name("totalMemory").value(totalMemory)
            .name("freeDisk").value(freeDisk)
            .name("orientation").value(orientation);


        // TODO migrate metadata values

        writer
            .name("batteryLevel").value(getBatteryLevel(appContext))
            .name("charging").value(isCharging(appContext))
            .name("locationStatus").value(getLocationStatus(appContext))
            .name("networkAccess").value(getNetworkAccess(appContext))
            .name("time").value(getTime())
            .name("brand").value(Build.BRAND)
            .name("apiLevel").value(Build.VERSION.SDK_INT)
            .name("osBuild").value(Build.DISPLAY)
            .name("locale").value(locale)
            .name("screenDensity").value(screenDensity)
            .name("dpi").value(dpi)
            .name("emulator").value(isEmulator())
            .name("screenResolution").value(screenResolution);

        writer.name("cpuAbi").beginArray();
        for (String s : cpuAbi) {
            writer.value(s);
        }
        writer.endArray();
        writer.endObject();
    }

    /**
     * @return the device's unique ID for the current app installation
     */
    @Nullable
    public String getId() {
        return id;
    }

    /**
     * Overrides the device's unique ID. This can be set to null for privacy reasons, if desired.
     *
     * @param id the new device id
     */
    public void setId(@Nullable String id) {
        this.id = id;
    }

    /**
     * @return the amount of free memory in bytes that the VM can allocate
     */
    public long getFreeMemory() {
        return freeMemory;
    }

    /**
     * Overrides the default value for the device's free memory.
     *
     * @param freeMemory the new free memory value, in bytes
     */
    public void setFreeMemory(long freeMemory) {
        this.freeMemory = freeMemory;
    }

    /**
     * @return the total amount of memory in bytes that the VM can allocate
     */
    public long getTotalMemory() {
        return totalMemory;
    }

    /**
     * Overrides the default value for the device's total memory.
     *
     * @param totalMemory the new total memory value, in bytes
     */
    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    /**
     * @return the amount of disk space available on the smallest disk on the device, if known
     */
    @Nullable
    public Long getFreeDisk() {
        return freeDisk;
    }

    /**
     * Overrides the default value for the device's free disk space, in bytes.
     *
     * @param freeDisk the new free disk space, in bytes
     */
    public void setFreeDisk(long freeDisk) {
        this.freeDisk = freeDisk;
    }

    /**
     * @return the device's orientation, if known
     */
    @Nullable
    public String getOrientation() {
        return orientation;
    }

    /**
     * Overrides the device's default orientation
     *
     * @param orientation the new orientation
     */
    public void setOrientation(@Nullable String orientation) {
        this.orientation = orientation;
    }


    /**
     * Guesses whether the current device is an emulator or not, erring on the side of caution
     *
     * @return true if the current device is an emulator
     */
    private boolean isEmulator() {
        String fingerprint = Build.FINGERPRINT;
        return fingerprint.startsWith("unknown")
            || fingerprint.contains("generic")
            || fingerprint.contains("vbox"); // genymotion
    }

    /**
     * The screen density scaling factor of the current Android device
     */
    @Nullable
    private static Float getScreenDensity(@NonNull Context appContext) {
        Resources resources = appContext.getResources();
        if (resources == null) {
            return null;
        }
        return resources.getDisplayMetrics().density;
    }

    /**
     * The screen density of the current Android device in dpi, eg. 320
     */
    @Nullable
    private static Integer getScreenDensityDpi(@NonNull Context appContext) {
        Resources resources = appContext.getResources();
        if (resources == null) {
            return null;
        }
        return resources.getDisplayMetrics().densityDpi;
    }

    /**
     * The screen resolution of the current Android device in px, eg. 1920x1080
     */
    @Nullable
    private static String getScreenResolution(@NonNull Context appContext) {
        Resources resources = appContext.getResources();
        if (resources == null) {
            return null;
        }
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int max = Math.max(metrics.widthPixels, metrics.heightPixels);
        int min = Math.min(metrics.widthPixels, metrics.heightPixels);
        return String.format(Locale.US, "%dx%d", max, min);
    }

    /**
     * Get the total memory available on the current Android device, in bytes
     */
    static long calculateTotalMemory() {
        if (Runtime.getRuntime().maxMemory() != Long.MAX_VALUE) {
            return Runtime.getRuntime().maxMemory();
        } else {
            return Runtime.getRuntime().totalMemory();
        }
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
            sharedPref.edit().putString(INSTALL_ID_KEY, installId).apply();
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


    /**
     * Get the free disk space on the smallest disk
     */
    @Nullable
    private static Long calculateFreeDisk() {
        try {
            StatFs externalStat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long externalBytesAvailable =
                (long) externalStat.getBlockSize() * (long) externalStat.getBlockCount();

            StatFs internalStat = new StatFs(Environment.getDataDirectory().getPath());
            long internalBytesAvailable =
                (long) internalStat.getBlockSize() * (long) internalStat.getBlockCount();

            return Math.min(internalBytesAvailable, externalBytesAvailable);
        } catch (Exception exception) {
            Logger.warn("Could not get freeDisk");
        }
        return null;
    }

    /**
     * Get the amount of memory remaining that the VM can allocate
     */
    private static long calculateFreeMemory() {
        Runtime runtime = Runtime.getRuntime();
        if (runtime.maxMemory() != Long.MAX_VALUE) {
            return runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
        } else {
            return runtime.freeMemory();
        }
    }

    /**
     * Get the device orientation, eg. "landscape"
     */
    @Nullable
    private static String calculateOrientation(@NonNull Context appContext) {
        String orientation;
        switch (appContext.getResources().getConfiguration().orientation) {
            case android.content.res.Configuration.ORIENTATION_LANDSCAPE:
                orientation = "landscape";
                break;
            case android.content.res.Configuration.ORIENTATION_PORTRAIT:
                orientation = "portrait";
                break;
            default:
                orientation = null;
                break;
        }
        return orientation;
    }

    /**
     * Get the current battery charge level, eg 0.3
     */
    @Nullable
    private static Float getBatteryLevel(@NonNull Context appContext) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = appContext.registerReceiver(null, ifilter);

            return batteryStatus.getIntExtra("level", -1)
                / (float) batteryStatus.getIntExtra("scale", -1);
        } catch (Exception exception) {
            Logger.warn("Could not get batteryLevel");
        }
        return null;
    }

    /**
     * Is the device currently charging/full battery?
     */
    @Nullable
    private static Boolean isCharging(@NonNull Context appContext) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = appContext.registerReceiver(null, ifilter);

            int status = batteryStatus.getIntExtra("status", -1);
            return (status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL);
        } catch (Exception exception) {
            Logger.warn("Could not get charging status");
        }
        return null;
    }

    /**
     * Get the current status of location services
     */
    @Nullable
    private static String getLocationStatus(@NonNull Context appContext) {
        try {
            ContentResolver cr = appContext.getContentResolver();
            String providersAllowed =
                Settings.Secure.getString(cr, Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if (providersAllowed != null && providersAllowed.length() > 0) {
                return "allowed";
            } else {
                return "disallowed";
            }
        } catch (Exception exception) {
            Logger.warn("Could not get locationStatus");
        }
        return null;
    }

    /**
     * Get the current status of network access, eg "cellular"
     */
    @Nullable
    private static String getNetworkAccess(@NonNull Context appContext) {
        try {
            ConnectivityManager cm =
                (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                if (activeNetwork.getType() == 1) {
                    return "wifi";
                } else if (activeNetwork.getType() == 9) {
                    return "ethernet";
                } else {
                    // We default to cellular as the other enums are all cellular in some
                    // form or another
                    return "cellular";
                }
            } else {
                return "none";
            }
        } catch (Exception exception) {
            Logger.warn("Could not get network access information, we "
                + "recommend granting the 'android.permission.ACCESS_NETWORK_STATE' permission");
        }
        return null;
    }

    /**
     * Get the current time on the device, in ISO8601 format.
     */
    @NonNull
    private String getTime() {
        return DateUtils.toIso8601(new Date());
    }

}
