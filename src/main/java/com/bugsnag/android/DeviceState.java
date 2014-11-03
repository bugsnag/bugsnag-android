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
class DeviceState implements JsonStreamer.Streamable {
    private Configuration config;
    private Context appContext;
    private String packageName;

    public DeviceState(Configuration config, Context appContext) {
        this.config = config;
        this.appContext = appContext;
        this.packageName = appContext.getPackageName();
    }

    public void toStream(JsonStreamer writer) {
        writer.beginObject()
            .name("freeMemory").value(freeMemory.get())
            .name("orientation").value(orientation.get())
            .name("batteryLevel").value(batteryLevel.get())
            .name("freeDisk").value(freeDisk.get())
            .name("charging").value(charging.get())
            .name("locationStatus").value(locationStatus.get())
            .name("networkAccess").value(networkAccess.get())
        .endObject();
    }

    /**
     * This is the amount of memory remaining that the VM can allocate
     */
    private SafeValue<Long> freeMemory = new SafeValue<Long>() {
        @Override
        public Long calc() {
            if(Runtime.getRuntime().maxMemory() != Long.MAX_VALUE) {
                return Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory();
            } else {
                return Runtime.getRuntime().freeMemory();
            }
        }
    };

    /**
     * Get the device orientation
     */
    private SafeValue<String> orientation = new SafeValue<String>() {
        @Override
        public String calc() {
            String orientation = null;
            switch(appContext.getResources().getConfiguration().orientation) {
                case android.content.res.Configuration.ORIENTATION_LANDSCAPE:
                    orientation = "landscape";
                case android.content.res.Configuration.ORIENTATION_PORTRAIT:
                    orientation = "portrait";
            }
            return orientation;
        }
    };

    /**
     * Get the battery charge level
     */
    private SafeValue<Float> batteryLevel = new SafeValue<Float>() {
        @Override
        public Float calc() {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = appContext.registerReceiver(null, ifilter);

            return batteryStatus.getIntExtra("level", -1) / (float)batteryStatus.getIntExtra("scale", -1);
        }
    };

    /**
     * Get the free disk space on the smallest disk
     */
    private SafeValue<Long> freeDisk = new SafeValue<Long>() {
        @Override
        public Long calc() {
            StatFs externalStat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long externalBytesAvailable = (long)externalStat.getBlockSize() * (long)externalStat.getBlockCount();

            StatFs internalStat = new StatFs(Environment.getDataDirectory().getPath());
            long internalBytesAvailable = (long)internalStat.getBlockSize() * (long)internalStat.getBlockCount();

            return Math.min(internalBytesAvailable, externalBytesAvailable);
        }
    };

    /**
     * Is the device currently charging/full batter?
     */
    private SafeValue<Boolean> charging = new SafeValue<Boolean>() {
        @Override
        public Boolean calc() {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = appContext.registerReceiver(null, ifilter);

            int status = batteryStatus.getIntExtra("status", -1);
            return (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
        }
    };

    /**
     * Get the current status of location services
     */
    private SafeValue<String> locationStatus = new SafeValue<String>() {
        @Override
        public String calc() {
            ContentResolver cr = appContext.getContentResolver();
            String providersAllowed = Settings.Secure.getString(cr, Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if(providersAllowed != null && providersAllowed.length() > 0) {
                return "allowed";
            } else {
                return "disallowed";
            }
        }
    };

    /**
     * Get the current status of network access
     */
    private SafeValue<String> networkAccess = new SafeValue<String>() {
        @Override
        public String calc() {
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
        }
    };
}
