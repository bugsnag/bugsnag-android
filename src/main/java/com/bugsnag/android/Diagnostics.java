package com.bugsnag.android;

import java.util.List;

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
    private final static long ONE_SECOND = 1000;
    private final static long SECONDS = 60;
    private final static long ONE_MINUTE = ONE_SECOND * 60;
    private final static long MINUTES = 60;
    private final static long ONE_HOUR = ONE_MINUTE * 60;
    private final static long HOURS = 24;
    private final static long ONE_DAY = ONE_HOUR * 24;

    private static final int[] INTERESTING_NETWORKS = new int[]{
        0, // TYPE_MOBILE
        1, // TYPE_WIFI
        6, // TYPE_WIMAX
        7, // TYPE_BLUETOOTH
        9, // TYPE_ETHERNET
    };

    private static Long startTime;

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
        addToTab("Session", "Session Length", durationString(SystemClock.elapsedRealtime() - startTime));
        addToTab("Device", "Time Since Boot", durationString(SystemClock.elapsedRealtime()));
        
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

    public static void startSessionTimer() {
        if(startTime == null) {
            startTime = SystemClock.elapsedRealtime();
        }
    }

    private static JSONObject getMemoryInfo(Context context) {
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

    private static String getNetworkStatus(Context context) {
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

    private static JSONObject getNetworks(Context context) {
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

    private static String durationString(long duration) {
        StringBuffer res = new StringBuffer();
        long temp = 0;
        if(duration >= ONE_SECOND) {
            temp = duration / ONE_DAY;
            if(temp > 0) {
                duration -= temp * ONE_DAY;
                res.append(temp).append(" day").append(temp > 1 ? "s" : "")
                   .append(duration >= ONE_MINUTE ? ", " : "");
            }

            temp = duration / ONE_HOUR;
            if(temp > 0) {
                duration -= temp * ONE_HOUR;
                res.append(temp).append(" hour").append(temp > 1 ? "s" : "")
                   .append(duration >= ONE_MINUTE ? ", " : "");
            }

            temp = duration / ONE_MINUTE;
            if(temp > 0) {
                duration -= temp * ONE_MINUTE;
                res.append(temp).append(" minute").append(temp > 1 ? "s" : "");
            }

            if(!res.toString().equals("") && duration >= ONE_SECOND) {
                res.append(" and ");
            }

            temp = duration / ONE_SECOND;
            if(temp > 0) {
                res.append(temp).append(" second").append(temp > 1 ? "s" : "");
            }

            return res.toString();
        } else {
            return "0 seconds";
        }
    }
}
