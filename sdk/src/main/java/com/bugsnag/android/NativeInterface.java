package com.bugsnag.android;

import static com.bugsnag.android.MapUtils.getStringFromMap;

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

    /**
     * Sets up observers for the NDK client
     * @param client the client
     */
    public static void configureClientObservers(@NonNull Client client) {

        // Ensure that the bugsnag observer is registered
        // Should only happen if the NDK library is present
        try {
            String className = "com.bugsnag.android.ndk.BugsnagObserver";
            Class<?> clz = Class.forName(className);
            Observer observer = (Observer) clz.newInstance();
            client.addObserver(observer);
        } catch (ClassNotFoundException exception) {
            // ignore this one, will happen if the NDK plugin is not present
            Logger.info("Bugsnag NDK integration not available");
        } catch (InstantiationException exception) {
            Logger.warn("Failed to instantiate NDK observer", exception);
        } catch (IllegalAccessException exception) {
            Logger.warn("Could not access NDK observer", exception);
        }

        // Should make NDK components configure
        client.notifyBugsnagObservers(NotifyType.ALL);
    }

    public static String getContext() {
        return getClient().getContext();
    }

    @Nullable
    public static String getErrorStorePath() {
        return getClient().errorStore.storeDirectory;
    }

    public static String getUserId() {
        return getClient().getUser().getId();
    }

    public static String getUserEmail() {
        return getClient().getUser().getEmail();
    }

    public static String getUserName() {
        return getClient().getUser().getName();
    }

    @Nullable
    public static String getPackageName() {
        return getStringFromMap("packageName", getClient().appData.getAppData());
    }

    @Nullable
    public static String getAppName() {
        return getClient().appData.appName;
    }

    @Nullable
    public static String getVersionName() {
        return getStringFromMap("version", getClient().appData.getAppData());
    }

    public static int getVersionCode() {
        Object versionCode = getClient().appData.getAppData().get("versionCode");
        return versionCode instanceof Integer ? (Integer) versionCode : -1;
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    public static String getBuildUUID() {
        return getClient().config.getBuildUUID();
    }

    @Nullable
    public static String getAppVersion() {
        return getStringFromMap("version", getClient().appData.getAppData());
    }

    public static String getReleaseStage() {
        return getStringFromMap("releaseStage", getClient().appData.getAppData());
    }

    @Nullable
    public static String getDeviceId() {
        return getStringFromMap("id", getClient().deviceData.getDeviceData());
    }

    @NonNull
    public static String getDeviceLocale() {
        return getClient().deviceData.locale;
    }

    public static double getDeviceTotalMemory() {
        return DeviceData.calculateTotalMemory();
    }

    /**
     * Returns whether a device is rooted or not to the NDK
     */
    public static Boolean getDeviceRooted() {
        Map<String, Object> map = getClient().deviceData.getDeviceDataSummary();
        Object jailbroken = map.get("jailbroken");
        return jailbroken instanceof Boolean ? (Boolean) jailbroken : false;
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

    /**
     * Retrieves the release stages
     * @return the release stages
     */
    @Nullable
    public static String[] getReleaseStages() {
        return getClient().config.getNotifyReleaseStages();
    }

    /**
     * Sets the user
     *
     * @param id id
     * @param email email
     * @param name name
     */
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

    /**
     * Notifies using the Android SDK
     *
     * @param name the error name
     * @param message the error message
     * @param severity the error severity
     * @param stacktrace a stacktrace
     * @param metaData any metadata
     */
    public static void notify(@NonNull final String name,
                              @NonNull final String message,
                              final Severity severity,
                              @NonNull final StackTraceElement[] stacktrace,
                              @NonNull final Map<String, Object> metaData) {

        getClient().notify(name, message, stacktrace, new Callback() {
            @Override
            public void beforeNotify(@NonNull Report report) {
                Error error = report.getError();
                error.setSeverity(severity);
                error.config.defaultExceptionType = "c";

                for (String tab : metaData.keySet()) {
                    Object value = metaData.get(tab);

                    if (value instanceof Map) {
                        @SuppressWarnings("unchecked") Map<Object, Object> map = (Map) value;

                        for (Object key : map.keySet()) {
                            error.getMetaData().addToTab(tab, key.toString(), map.get(key));
                        }
                    } else {
                        error.getMetaData().addToTab("custom", tab, value);
                    }
                }
            }
        });
    }
}
