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
    public enum MessageType {
        /**
         * Add a breadcrumb. The Message object should be the breadcrumb
         */
        ADD_BREADCRUMB,
        /**
         * Add a new metadata value. The Message object should be an array containing [tab, key, value]
         */
        ADD_METADATA,
        /**
         * Clear all breadcrumbs
         */
        CLEAR_BREADCRUMBS,
        /**
         * Clear all metadata on a tab. The Message object should be the tab name
         */
        CLEAR_METADATA_TAB,
        /**
         * Set up Bugsnag. The message object should be a Configuration.
         */
        INSTALL,
        /**
         * Send a report for a handled Java exception
         */
        NOTIFY_HANDLED,
        /**
         * Remove a metadata value. The Message object should be a string array containing [tab, key]
         */
        REMOVE_METADATA,
        /**
         * A new session was started. The Message object should be a string array containing [id, startDate]
         */
        START_SESSION,
        /**
         * Set a new app version. The Message object should be the new app version
         */
        UPDATE_APP_VERSION,
        /**
         * Set a new build UUID. The Message object should be the new build UUID string
         */
        UPDATE_BUILD_UUID,
        /**
         * Set a new context. The message object should be the new context
         */
        UPDATE_CONTEXT,
        /**
         * Set a new value for `app.inForeground`. The message object should be a Boolean
         */
        UPDATE_IN_FOREGROUND,
        /**
         * Set a new value for `app.lowMemory`. The message object should be a Boolean
         */
        UPDATE_LOW_MEMORY,
        /**
         * Set a new value for all custom metadata. The message object should be MetaData.
         */
        UPDATE_METADATA,
        /**
         * Set a new value for `device.orientation`. The message object should be the orientation
         */
        UPDATE_ORIENTATION,
        /**
         * Set a new value for `app.releaseStage`. The message object should be the new release stage
         */
        UPDATE_RELEASE_STAGE,
        /**
         * Set a new value for user email. The message object is a string array containing [id, email, name]
         */
        UPDATE_USER_EMAIL,
        /**
         * Set a new value for user name. The message object is a string array containing [id, email, name]
         */
        UPDATE_USER_NAME,
        /**
         * Set a new value for user id. The message object is a string array containing [id, email, name]
         */
        UPDATE_USER_ID,
    }

    /**
     * Wrapper for messages sent to native observers
     */
    public static class Message {
        public MessageType type;
        public Object value;

        public Message(MessageType type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

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
        if (NativeInterface.client == client) return;
        NativeInterface.client = client;
        configureClientObservers(client);
    }

    /**
     * Sets up observers for the NDK client
     * @param client the client
     */
    public static void configureClientObservers(@NonNull Client client) {
        try {
            String className = "com.bugsnag.android.ndk.NativeBridge";
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

        // Configure NDK components
        client.sendNativeSetupNotification();
    }

    public static String getContext() {
        return getClient().getContext();
    }

    public static String getNativeReportPath() {
        return getClient().appContext.getCacheDir().getAbsolutePath() + "/bugsnag-native/";
    }

    public static Map<String,String> getUserData() {
        HashMap<String, String> userData = new HashMap<>();
        User user = getClient().getUser();
        userData.put("id", user.getId());
        userData.put("name", user.getName());
        userData.put("email", user.getEmail());

        return userData;
    }

    public static Map<String,Object> getAppData() {
        HashMap<String,Object> data = new HashMap<>();
        AppData source = getClient().getAppData();
        data.putAll(source.getAppData());
        data.putAll(source.getAppDataMetaData());
        data.putAll(source.getAppDataSummary());
        return data;
    }

    @NonNull
    public static Map<String,Object> getDeviceData() {
        HashMap<String,Object> deviceData = new HashMap<>();
        DeviceData source = getClient().getDeviceData();
        deviceData.putAll(source.getDeviceMetaData());
        deviceData.putAll(source.getDeviceDataSummary());
        deviceData.putAll(source.getDeviceData()); // wat
        return deviceData;
    }

    @NonNull
    public static Map<String, Object> getMetaData() {
        return getClient().getMetaData().store;
    }

    public static Object[] getBreadcrumbs() {
        return getClient().breadcrumbs.store.toArray();
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

        getClient().setUserId(id);
        getClient().setUserEmail(email);
        getClient().setUserName(name);
    }

    public static void leaveBreadcrumb(@NonNull final String name,
                                       @NonNull final BreadcrumbType type) {
        getClient().leaveBreadcrumb(name, type, new HashMap<String, String>());
    }

    public static void addToTab(final String tab,
                                final String key,
                                final Object value) {
        getClient().addToTab(tab, key, value);
    }

    public static void deliverReport(String releaseStage, String payload) {
        if (releaseStage == null || releaseStage.length() == 0 || getClient().getConfig().shouldNotifyForReleaseStage(releaseStage)) {
            getClient().getErrorStore().enqueueContentForDelivery(payload);
            getClient().getErrorStore().flushAsync();
        }
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
