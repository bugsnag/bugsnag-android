package com.bugsnag.android;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.provider.Settings;

import com.bugsnag.MetaData;
import com.bugsnag.utils.JSONUtils;

class Diagnostics extends MetaData {
    private static final int[] INTERESTING_NETWORKS = new int[]{
        0, // TYPE_MOBILE
        1, // TYPE_WIFI
        6, // TYPE_WIMAX
        7, // TYPE_BLUETOOTH
        9, // TYPE_ETHERNET
    };

    private static long startTime = secondsSinceBoot();

    public Diagnostics(Context context) {
        // Activity stack
        String topActivityName = ActivityStack.getTopActivityName();
        List<String> activityStackNames = ActivityStack.getNames();
        if(activityStackNames.size() > 0) {
            addToTab("Application", "Activity Stack", activityStackNames);
        }

        // Top activity
        if(topActivityName != null) {
            addToTab("Application", "Top Activity", topActivityName);
        }
 
        // Time since boot and app start
        addToTab("Session", "Session Length", durationString(secondsSinceBoot() - startTime));
        addToTab("Device", "Seconds Since Boot", durationString(secondsSinceBoot()));
        
        // Network status
        String networkStatus = getNetworkStatus(context);
        if(networkStatus != null) {
            addToTab("Device", "Network Status", networkStatus);
        }

        // Networks
        JSONObject networks = getNetworks(context);
        if(networks != null) {
            addToTab("Device", "Networks", networks);
        }

        // GPS status
        Boolean gpsEnabled = getGpsEnabled(context);
        if(gpsEnabled != null) {
            addToTab("Device", "GPS Enabled?", gpsEnabled);
        }

        // Current memory status
        JSONObject memoryInfo = getMemoryInfo(context);
        if(memoryInfo != null) {
            addToTab("Device", "Memory", memoryInfo);
        }
    }

    public static long secondsSinceBoot() {
        return (long)(SystemClock.elapsedRealtime()/1000);
    }

    public static JSONObject getMemoryInfo(Context context) {
        JSONObject memoryInfo = null;

        try {
            ActivityManager activityManager = (ActivityManager)context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memInfo);

            long totalMem = Runtime.getRuntime().maxMemory();
            long usedMem = Runtime.getRuntime().totalMemory();
            long freeMem = totalMem - usedMem;

            memoryInfo = new JSONObject();
            JSONUtils.safePut(memoryInfo, "Total Available", humanReadableByteCount(totalMem));
            JSONUtils.safePut(memoryInfo, "Free", humanReadableByteCount(freeMem));
            JSONUtils.safePut(memoryInfo, "Used", humanReadableByteCount(usedMem));
            JSONUtils.safePut(memoryInfo, "Low Memory?", memInfo.lowMemory);
        } catch(Exception e) {
            e.printStackTrace();
        }

        return memoryInfo;
    }

    public static String getNetworkStatus(Context context) {
        String networkStatus = "Unknown";

        try {
            // Get the network information
            ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if(activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                networkStatus = "Connected";
            } else {
                networkStatus = "Not Connected";
            }
        } catch(SecurityException e) {
            // App doesn't have android.permission.ACCESS_NETWORK_STATE permission
        } catch(Exception e) {
            e.printStackTrace();
        }

        return networkStatus;
    }

    public static JSONObject getNetworks(Context context) {
        JSONObject networks = null;

        try {
            // Get the network information
            ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
            NetworkInfo[] allNetworkInfo = cm.getAllNetworkInfo();

            // Loop through all networks
            networks = new JSONObject();
            for(int networkType : INTERESTING_NETWORKS) {
                NetworkInfo networkInfo = cm.getNetworkInfo(networkType);
                if(networkInfo != null) {
                    JSONObject network = new JSONObject();
                    JSONUtils.safePut(network, "Connected", networkInfo.isConnectedOrConnecting());
                    if(networkInfo.getSubtypeName() != null && networkInfo.getSubtypeName().length() > 0) {
                        JSONUtils.safePut(network, "Type", networkInfo.getSubtypeName());
                    }
                    if(activeNetworkInfo != null && networkInfo.getType() == activeNetworkInfo.getType()) {
                        JSONUtils.safePut(network, "Active", true);
                    }

                    JSONUtils.safePut(networks, networkInfo.getTypeName(), network);
                }
            }

            // Return null if empty
            if(networks.length() == 0) {
                networks = null;
            }
        } catch(SecurityException e) {
            // App doesn't have android.permission.ACCESS_NETWORK_STATE permission
        } catch(Exception e) {
            e.printStackTrace();
        }

        return networks;
    }

    private static Boolean getGpsEnabled(Context context) {
        Boolean gpsEnabled = null;

        try {
            ContentResolver cr = context.getContentResolver();
            String providersAllowed = Settings.Secure.getString(cr, Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            gpsEnabled = providersAllowed != null && providersAllowed.length() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return gpsEnabled;
    }

    private static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp-1) + "i";

        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private static String durationString(long durationSeconds) {
        String returnValue = "";
        int entries = 0;
        long currentSeconds = durationSeconds;
        long result;
        
        result = TimeUnit.SECONDS.toDays(currentSeconds);
        if(result != 0) {
            currentSeconds -= TimeUnit.DAYS.toSeconds(result);
            returnValue += returnValue.format("%d Days", result);
            entries++;
        }
        
        result = TimeUnit.SECONDS.toHours(currentSeconds);
        if(result != 0 && entries <= 1) {
            if(entries == 1) returnValue += " and ";
            currentSeconds -= TimeUnit.HOURS.toSeconds(result);
            returnValue += returnValue.format("%d Hours", result);
            entries++;
        }
        
        result = TimeUnit.SECONDS.toMinutes(currentSeconds);
        
        if(result != 0 && entries <= 1) {
            if(entries == 1) returnValue += " and ";
            currentSeconds -= TimeUnit.MINUTES.toSeconds(result);
            returnValue += returnValue.format("%d Minutes", result);
            entries++;
        }
        
        result = currentSeconds;
        if(result != 0 && entries <= 1) {
            if(entries == 1) returnValue += " and ";
            returnValue += returnValue.format("%d Seconds", result);
            entries++;
        }

        if(result == 0 && entries == 0) {
            returnValue = "0 Seconds";
        }

        return returnValue;
    }
}