package com.bugsnag.android;

import java.sql.BatchUpdateException;
import java.util.HashMap;
import java.util.Map;

/**
 * Used as the entry point for native code to allow proguard to obfuscate other areas if needed
 */
class NativeInterface {

    public static String getContext() {
        return Bugsnag.getClient().getContext();
    }

    public static String getErrorStorePath() {
        return Bugsnag.getClient().errorStore.path;
    }

    public static String getUserId() {
        return Bugsnag.getClient().user.getId();
    }

    public static String getUserEmail() {
        return Bugsnag.getClient().user.getEmail();
    }

    public static String getUserName() {
        return Bugsnag.getClient().user.getName();
    }

    public static String getPackageName() {
        return Bugsnag.getClient().appData.packageName;
    }

    public static String getAppName() {
        return Bugsnag.getClient().appData.appName;
    }

    public static String getVersionName() {
        return Bugsnag.getClient().appData.versionName;
    }

    public static int getVersionCode() {
        return Bugsnag.getClient().appData.versionCode;
    }

    public static String getBuildUUID() {
        return Bugsnag.getClient().config.getBuildUUID();
    }

    public static String getAppVersion() {
        return Bugsnag.getClient().appData.getAppVersion();
    }

    public static String getReleaseStage() {
        return Bugsnag.getClient().appData.getReleaseStage();
    }

    public static String getDeviceId() {
        return Bugsnag.getClient().deviceData.id;
    }

    public static String getDeviceLocale() {
        return Bugsnag.getClient().deviceData.locale;
    }

    public static double getDeviceTotalMemory() {
        return Bugsnag.getClient().deviceData.totalMemory;
    }

    public static Boolean getDeviceRooted() {
        return Bugsnag.getClient().deviceData.rooted;
    }

    public static float getDeviceScreenDensity() {
        return Bugsnag.getClient().deviceData.screenDensity;
    }

    public static int getDeviceDpi() {
        return Bugsnag.getClient().deviceData.dpi;
    }

    public static String getDeviceScreenResolution() {
        return Bugsnag.getClient().deviceData.screenResolution;
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

    public static String[] getDeviceCpuAbi() {
        return Bugsnag.getClient().deviceData.cpuAbi;
    }


    public static Map<String, Object> getMetaData() {
        return Bugsnag.getClient().getMetaData().store;
    }

    public static Object[] getBreadcrumbs() {
        return Bugsnag.getClient().breadcrumbs.store.toArray();
    }

    public static String[] getFilters() {
        return Bugsnag.getClient().config.getFilters();
    }

    public static String[] getReleaseStages() {
        return Bugsnag.getClient().config.getNotifyReleaseStages();
    }

    public static void setUser(final String id,
                               final String email,
                               final String name) {

        Bugsnag.getClient().setUserId(id, false);
        Bugsnag.getClient().setUserEmail(email, false);
        Bugsnag.getClient().setUserName(name, false);
    }

    public static void leaveBreadcrumb(final String name,
                                       final BreadcrumbType type) {

        Bugsnag.getClient().leaveBreadcrumb(name, type, new HashMap<String, String>(), false);
    }

    public static void addToTab(final String tab,
                                final String key,
                                final Object value) {

        Bugsnag.getClient().config.getMetaData().addToTab(tab, key, value, false);
    }

    public static void notify(final String name,
                              final String message,
                              final Severity severity,
                              final StackTraceElement[] stacktrace,
                              final Map<String, Object> metaData) {

        Bugsnag.getClient().notify(name, message, stacktrace, new Callback() {
            @Override
            public void beforeNotify(Report report) {
                report.getError().setSeverity(severity);
                report.getError().config.defaultExceptionType = "c";

                for (String tab : metaData.keySet()) {

                    Object value = metaData.get(tab);

                    if (value instanceof Map) {
                        Map map = (Map)value;

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
