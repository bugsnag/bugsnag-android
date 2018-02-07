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
class DeviceData extends DeviceDataSummary {

    private static final String INSTALL_ID_KEY = "install.iud";

    @Nullable
    final Float screenDensity;

    @Nullable
    final Integer dpi;

    @Nullable
    final String screenResolution;
    private Context appContext;

    @NonNull
    final String locale;

    @Nullable
    protected String id;

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
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        serialiseMinimalDeviceData(writer);

        writer
            .name("id").value(id)
            .name("freeMemory").value(getFreeMemory())
            .name("totalMemory").value(getTotalMemory())
            .name("freeDisk").value(getFreeDisk())
            .name("orientation").value(getOrientation(appContext));


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
            .name("screenResolution").value(screenResolution);

        writer.name("cpuAbi").beginArray();
        for (String s : cpuAbi) {
            writer.value(s);
        }
        writer.endArray();
        writer.endObject();
    }

    @NonNull
    String getUserId() {
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
    @NonNull
    static Long getTotalMemory() {
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
    private static Long getFreeDisk() {
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
    @NonNull
    private static Long getFreeMemory() {
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
    private static String getOrientation(@NonNull Context appContext) {
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
        return DateUtils.toISO8601(new Date());
    }

}
