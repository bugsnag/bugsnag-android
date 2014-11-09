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

/**
 * This class contains information about the curremt device which changes
 * over time.
 */
class DeviceState implements JsonStream.Streamable {
    private Configuration config;
    private Context appContext;
    private String packageName;

    DeviceState(Configuration config, Context appContext) {
        this.config = config;
        this.appContext = appContext;
        this.packageName = appContext.getPackageName();
    }

    public void toStream(JsonStream writer) {
        writer.beginObject()
            .name("freeMemory").value(getFreeMemory())
            .name("orientation").value(getOrientation())
            .name("batteryLevel").value(getBatteryLevel())
            .name("freeDisk").value(getFreeDisk())
            .name("charging").value(isCharging())
            .name("locationStatus").value(getLocationStatus())
            .name("networkAccess").value(getNetworkAccess())
        .endObject();
    }

    /**
     * This is the amount of memory remaining that the VM can allocate
     */
    private Long getFreeMemory() {
        try {
            if(Runtime.getRuntime().maxMemory() != Long.MAX_VALUE) {
                return Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory();
            } else {
                return Runtime.getRuntime().freeMemory();
            }
        } catch (Exception e) {
            Logger.warn("Could not get freeMemory");
        }
        return null;
    }

    /**
     * Get the device orientation
     */
    private String getOrientation() {
        try {
            String orientation = null;
            switch(appContext.getResources().getConfiguration().orientation) {
                case android.content.res.Configuration.ORIENTATION_LANDSCAPE:
                    orientation = "landscape";
                case android.content.res.Configuration.ORIENTATION_PORTRAIT:
                    orientation = "portrait";
            }
            return orientation;
        } catch (Exception e) {
            Logger.warn("Could not get orientation");
        }
        return null;
    }

    /**
     * Get the battery charge level
     */
    private Float getBatteryLevel() {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = appContext.registerReceiver(null, ifilter);

            return batteryStatus.getIntExtra("level", -1) / (float)batteryStatus.getIntExtra("scale", -1);
        } catch (Exception e) {
            Logger.warn("Could not get batteryLevel");
        }
        return null;
    }

    /**
     * Get the free disk space on the smallest disk
     */
    private Long getFreeDisk() {
        try {
            StatFs externalStat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long externalBytesAvailable = (long)externalStat.getBlockSize() * (long)externalStat.getBlockCount();

            StatFs internalStat = new StatFs(Environment.getDataDirectory().getPath());
            long internalBytesAvailable = (long)internalStat.getBlockSize() * (long)internalStat.getBlockCount();

            return Math.min(internalBytesAvailable, externalBytesAvailable);
        } catch (Exception e) {
            Logger.warn("Could not get freeDisk");
        }
        return null;
    }

    /**
     * Is the device currently charging/full batter?
     */
    private Boolean isCharging() {
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
    private String getLocationStatus() {
        try {
            ContentResolver cr = appContext.getContentResolver();
            String providersAllowed = Settings.Secure.getString(cr, Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if(providersAllowed != null && providersAllowed.length() > 0) {
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
     * Get the current status of network access
     */
    private String getNetworkAccess() {
        try {
            ConnectivityManager cm = (ConnectivityManager)appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if(activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                if(activeNetwork.getType() == 1) {
                    return "wifi";
                } else if(activeNetwork.getType() == 9) {
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
}
