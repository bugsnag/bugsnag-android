package com.bugsnag.android;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.util.DisplayMetrics;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

class DeviceData {

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

    private static final String INSTALL_ID_KEY = "install.iud";

    private final boolean emulator;
    private final Context appContext;
    private final Connectivity connectivity;
    private final Resources resources;
    private final SharedPreferences sharedPrefs;
    private final DisplayMetrics displayMetrics;
    private final String id;
    private final boolean rooted;

    @Nullable
    final Float screenDensity;

    @Nullable
    final Integer dpi;

    @Nullable
    final String screenResolution;

    @NonNull
    final String locale;

    @NonNull
    final String[] cpuAbi;

    DeviceData(Connectivity connectivity, Context appContext, Resources resources,
               SharedPreferences sharedPreferences) {
        this.connectivity = connectivity;
        this.appContext = appContext;
        this.resources = resources;
        this.sharedPrefs = sharedPreferences;

        if (resources != null) {
            displayMetrics = resources.getDisplayMetrics();
        } else {
            displayMetrics = null;
        }

        screenDensity = getScreenDensity();
        dpi = getScreenDensityDpi();
        screenResolution = getScreenResolution();
        locale = getLocale();
        cpuAbi = getCpuAbi();
        emulator = isEmulator();
        id = retrieveUniqueInstallId();
        rooted = isRooted();
    }

    Map<String, Object> getDeviceDataSummary() {
        Map<String, Object> map = new HashMap<>();
        map.put("manufacturer", Build.MANUFACTURER);
        map.put("model", Build.MODEL);
        map.put("jailbroken", rooted);
        map.put("osName", "android");
        map.put("osVersion", Build.VERSION.RELEASE);
        map.put("cpuAbi", cpuAbi);

        Map<String, Object> versions = new HashMap<>();
        versions.put("androidApiLevel", Build.VERSION.SDK_INT);
        versions.put("osBuild", Build.DISPLAY);
        map.put("runtimeVersions", versions);
        return map;
    }

    Map<String, Object> getDeviceData() {
        Map<String, Object> map = getDeviceDataSummary();
        map.put("id", id);
        map.put("freeMemory", calculateFreeMemory());
        map.put("totalMemory", calculateTotalMemory());
        map.put("freeDisk", calculateFreeDisk());
        map.put("orientation", calculateOrientation());
        return map;
    }

    Map<String, Object> getDeviceMetaData() {
        Map<String, Object> map = new HashMap<>();
        map.put("batteryLevel", getBatteryLevel());
        map.put("charging", isCharging());
        map.put("locationStatus", getLocationStatus());
        map.put("networkAccess", getNetworkAccess());
        map.put("time", getTime());
        map.put("brand", Build.BRAND);
        map.put("locale", locale);
        map.put("screenDensity", screenDensity);
        map.put("dpi", dpi);
        map.put("emulator", emulator);
        map.put("screenResolution", screenResolution);
        return map;
    }

    String getId() {
        return id;
    }

    /**
     * Check if the current Android device is rooted
     */
    private boolean isRooted() {
        if (android.os.Build.TAGS != null && android.os.Build.TAGS.contains("test-keys")) {
            return true;
        }

        try {
            for (String candidate : ROOT_INDICATORS) {
                if (new File(candidate).exists()) {
                    return true;
                }
            }
        } catch (Exception ignore) {
            return false;
        }
        return false;
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
    private Float getScreenDensity() {
        if (displayMetrics != null) {
            return displayMetrics.density;
        } else {
            return null;
        }
    }

    /**
     * The screen density of the current Android device in dpi, eg. 320
     */
    @Nullable
    private Integer getScreenDensityDpi() {
        if (displayMetrics != null) {
            return displayMetrics.densityDpi;
        } else {
            return null;
        }
    }

    /**
     * The screen resolution of the current Android device in px, eg. 1920x1080
     */
    @Nullable
    private String getScreenResolution() {
        if (displayMetrics != null) {
            int max = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);
            int min = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
            return String.format(Locale.US, "%dx%d", max, min);
        } else {
            return null;
        }
    }

    /**
     * Get the total memory available on the current Android device, in bytes
     */
    static long calculateTotalMemory() {
        Runtime runtime = Runtime.getRuntime();
        if (runtime.maxMemory() != Long.MAX_VALUE) {
            return runtime.maxMemory();
        } else {
            return runtime.totalMemory();
        }
    }

    /**
     * Get the locale of the current Android device, eg en_US
     */
    @NonNull
    private String getLocale() {
        return Locale.getDefault().toString();
    }

    /**
     * Get the unique id for the current app installation, creating a unique UUID if needed
     */
    @Nullable
    private String retrieveUniqueInstallId() {
        String installId = sharedPrefs.getString(INSTALL_ID_KEY, null);

        if (installId == null) {
            installId = UUID.randomUUID().toString();
            sharedPrefs.edit().putString(INSTALL_ID_KEY, installId).apply();
        }
        return installId;
    }

    /**
     * Gets information about the CPU / API
     */
    @NonNull
    private String[] getCpuAbi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return SupportedAbiWrapper.getSupportedAbis();
        }
        return Abi2Wrapper.getAbi1andAbi2();
    }

    /**
     * Get the usable disk space on internal storage's data directory
     */
    @SuppressLint("UsableSpace")
    long calculateFreeDisk() {
        // for this specific case we want the currently usable space, not
        // StorageManager#allocatableBytes() as the UsableSpace lint inspection suggests
        File dataDirectory = Environment.getDataDirectory();
        return dataDirectory.getUsableSpace();
    }

    /**
     * Get the amount of memory remaining that the VM can allocate
     */
    private long calculateFreeMemory() {
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
    private String calculateOrientation() {
        String orientation = null;

        if (resources != null) {
            int i = resources.getConfiguration().orientation;
            if (i == Configuration.ORIENTATION_LANDSCAPE) {
                orientation = "landscape";
            } else if (i == Configuration.ORIENTATION_PORTRAIT) {
                orientation = "portrait";
            }
        }
        return orientation;
    }

    /**
     * Get the current battery charge level, eg 0.3
     */
    @Nullable
    private Float getBatteryLevel() {
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
    private Boolean isCharging() {
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
    @SuppressWarnings("deprecation") // LOCATION_PROVIDERS_ALLOWED is deprecated
    private String getLocationStatus() {
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
    private String getNetworkAccess() {
        return connectivity.retrieveNetworkAccessState();
    }

    /**
     * Get the current time on the device, in ISO8601 format.
     */
    @NonNull
    private String getTime() {
        return DateUtils.toIso8601(new Date());
    }

    /**
     * Wrapper class to allow the test framework to use the correct version of the CPU / ABI
     */
    static class SupportedAbiWrapper {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        static String[] getSupportedAbis() {
            return Build.SUPPORTED_ABIS;
        }
    }

    /**
     * Wrapper class to allow the test framework to use the correct version of the CPU / ABI
     */
    static class Abi2Wrapper {
        @NonNull
        @SuppressWarnings("deprecation") // new API already used elsewhere
        static String[] getAbi1andAbi2() {
            return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        }
    }
}
