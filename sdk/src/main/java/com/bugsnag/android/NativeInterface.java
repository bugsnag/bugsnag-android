package com.bugsnag.android;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Observer;

/**
 * Used as the entry point for native code to allow proguard to obfuscate other areas if needed
 */
public class NativeInterface {

    /**
     * Static reference used if not using Bugsnag.init()
     */
    @SuppressLint("StaticFieldLeak")
    private static Client client;

    @NonNull
    private static Client getClient() {
        if (client != null) {
            return client;
        } else {
            return Bugsnag.getClient();
        }
    }

    public static void setClient(@NonNull Client client) {
        NativeInterface.client = client;
        configureClientObservers(client);
    }

    public static void configureClientObservers(@NonNull Client client) {

        // Ensure that the bugsnag observer is registered
        // Should only happen if the NDK library is present
        try {
            String className = "com.bugsnag.android.ndk.BugsnagObserver";
            Class c = Class.forName(className);
            Observer o = (Observer) c.newInstance();
            client.addObserver(o);
        } catch (ClassNotFoundException e) {
            // ignore this one, will happen if the NDK plugin is not present
            Logger.info("Bugsnag NDK integration not available");
        } catch (InstantiationException e) {
            Logger.warn("Failed to instantiate NDK observer", e);
        } catch (IllegalAccessException e) {
            Logger.warn("Could not access NDK observer", e);
        }

        // Should make NDK components configure
        client.notifyBugsnagObservers(NotifyType.ALL);
    }

    public static String getContext() {
        return getClient().getContext();
    }

    @Nullable
    public static String getErrorStorePath() {
        return getClient().errorStore.path;
    }

    public static String getUserId() {
        return getClient().user.getId();
    }

    public static String getUserEmail() {
        return getClient().user.getEmail();
    }

    public static String getUserName() {
        return getClient().user.getName();
    }

    @NonNull
    public static String getPackageName() {
        return getClient().appData.packageName;
    }

    @Nullable
    public static String getAppName() {
        return getClient().appData.appName;
    }

    @Nullable
    public static String getVersionName() {
        return getClient().appData.versionName;
    }

    public static int getVersionCode() {
        return getClient().appData.versionCode;
    }

    public static String getBuildUUID() {
        return getClient().config.getBuildUUID();
    }

    @Nullable
    public static String getAppVersion() {
        return getClient().appData.getAppVersion();
    }

    public static String getReleaseStage() {
        return getClient().appData.getReleaseStage();
    }

    @Nullable
    public static String getDeviceId() {
        return getClient().deviceData.id;
    }

    @NonNull
    public static String getDeviceLocale() {
        return getClient().deviceData.locale;
    }

    public static double getDeviceTotalMemory() {
        return getClient().deviceData.totalMemory;
    }

    @Nullable
    public static Boolean getDeviceRooted() {
        return getClient().deviceData.rooted;
    }

    public static float getDeviceScreenDensity() {
        return getClient().deviceData.screenDensity;
    }

    public static int getDeviceDpi() {
        return getClient().deviceData.dpi;
    }

    @Nullable
    public static String getDeviceScreenResolution() {
        return getClient().deviceData.screenResolution;
    }

    public static String getDeviceManufacturer() {
        return android.os.Build.MANUFACTURER;
    }

    public static String getDeviceBrand() {
        return android.os.Build.BRAND;
    }

    public static String getDeviceModel() {
        return android.os.Build.MODEL;
    }

    public static String getDeviceOsVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    public static String getDeviceOsBuild() {
        return android.os.Build.DISPLAY;
    }

    public static int getDeviceApiLevel() {
        return android.os.Build.VERSION.SDK_INT;
    }

    @NonNull
    public static String[] getDeviceCpuAbi() {
        return getClient().deviceData.cpuAbi;
    }


    @NonNull
    public static Map<String, Object> getMetaData() {
        return getClient().getMetaData().store;
    }

    public static Object[] getBreadcrumbs() {
        return getClient().breadcrumbs.store.toArray();
    }

    public static String[] getFilters() {
        return getClient().config.getFilters();
    }

    @Nullable
    public static String[] getReleaseStages() {
        return getClient().config.getNotifyReleaseStages();
    }

    public static void setUser(final String id,
                               final String email,
                               final String name) {

        getClient().setUserId(id, false);
        getClient().setUserEmail(email, false);
        getClient().setUserName(name, false);
    }

    public static void leaveBreadcrumb(@NonNull final String name,
                                       @NonNull final BreadcrumbType type) {

        getClient().leaveBreadcrumb(name, type, new HashMap<String, String>(), false);
    }

    public static void addToTab(final String tab,
                                final String key,
                                final Object value) {

        getClient().config.getMetaData().addToTab(tab, key, value, false);
    }

    public static void notify(@NonNull final String name,
                              @NonNull final String message,
                              final Severity severity,
                              @NonNull final StackTraceElement[] stacktrace,
                              @NonNull final Map<String, Object> metaData) {

        getClient().notify(name, message, stacktrace, new Callback() {
            @Override
            public void beforeNotify(@NonNull Report report) {
                report.getError().setSeverity(severity);
                report.getError().config.defaultExceptionType = "c";

                for (String tab : metaData.keySet()) {

                    Object value = metaData.get(tab);

                    if (value instanceof Map) {
                        Map map = (Map) value;

                        for (Object key : map.keySet()) {
                            report.getError().getMetaData().addToTab(tab, key.toString(), map.get(key));
                        }
                    } else {
                        report.getError().getMetaData().addToTab("custom", tab, value);
                    }
                }
            }
        });
    }
}
