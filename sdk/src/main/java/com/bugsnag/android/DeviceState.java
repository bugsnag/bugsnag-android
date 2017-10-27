package com.bugsnag.android;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.Date;

/**
 * Information about the current Android device which can change over time,
 * including free memory, sensor status and orientation.
 * <p/>
 * App information in this class is not cached, and is recalculated every
 * time toStream is called.
 */
class DeviceState implements JsonStream.Streamable {
    @NonNull
    private final Long freeMemory;
    @Nullable
    private final String orientation;
    @Nullable
    private final Float batteryLevel;
    @Nullable
    private final Long freeDisk;
    @Nullable
    private final Boolean charging;
    @Nullable
    private final String locationStatus;
    @Nullable
    private final String networkAccess;
    @NonNull
    private final String time;

    DeviceState(@NonNull Context appContext) {

        freeMemory = getFreeMemory();
        orientation = getOrientation(appContext);
        batteryLevel = getBatteryLevel(appContext);
        freeDisk = getFreeDisk();
        charging = isCharging(appContext);
        locationStatus = getLocationStatus(appContext);
        networkAccess = getNetworkAccess(appContext);
        time = getTime();
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();

        writer.name("freeMemory").value(freeMemory);
        writer.name("orientation").value(orientation);
        writer.name("batteryLevel").value(batteryLevel);
        writer.name("freeDisk").value(freeDisk);
        writer.name("charging").value(charging);
        writer.name("locationStatus").value(locationStatus);
        writer.name("networkAccess").value(networkAccess);
        writer.name("time").value(time);

        writer.endObject();
    }

    /**
     * Get the amount of memory remaining that the VM can allocate
     */
    @NonNull
    private static Long getFreeMemory() {
        if (Runtime.getRuntime().maxMemory() != Long.MAX_VALUE) {
            return Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory();
        } else {
            return Runtime.getRuntime().freeMemory();
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

            return batteryStatus.getIntExtra("level", -1) / (float) batteryStatus.getIntExtra("scale", -1);
        } catch (Exception e) {
            Logger.warn("Could not get batteryLevel");
        }
        return null;
    }

    /**
     * Get the free disk space on the smallest disk
     */
    @Nullable
    private static Long getFreeDisk() {
        try {
            StatFs externalStat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long externalBytesAvailable = (long) externalStat.getBlockSize() * (long) externalStat.getBlockCount();

            StatFs internalStat = new StatFs(Environment.getDataDirectory().getPath());
            long internalBytesAvailable = (long) internalStat.getBlockSize() * (long) internalStat.getBlockCount();

            return Math.min(internalBytesAvailable, externalBytesAvailable);
        } catch (Exception e) {
            Logger.warn("Could not get freeDisk");
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
            return (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
        } catch (Exception e) {
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
            String providersAllowed = Settings.Secure.getString(cr, Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if (providersAllowed != null && providersAllowed.length() > 0) {
                return "allowed";
            } else {
                return "disallowed";
            }
        } catch (Exception e) {
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
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
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
        } catch (Exception e) {
            Logger.warn("Could not get network access information, we recommend granting the 'android.permission.ACCESS_NETWORK_STATE' permission");
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
