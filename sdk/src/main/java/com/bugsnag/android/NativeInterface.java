package com.bugsnag.android;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observer;
import java.util.Queue;

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
         * Add a new metadata value. The Message object should be an array
         * containing [tab, key, value]
         */
        ADD_METADATA,
        /**
         * Clear all breadcrumbs
         */
        CLEAR_BREADCRUMBS,
        /**
         * Clear all metadata on a tab. The Message object should be the tab
         * name
         */
        CLEAR_METADATA_TAB,
        /**
         * Deliver all pending reports
         */
        DELIVER_PENDING,
        /**
         * Set up Bugsnag. The message object should be a Configuration.
         */
        INSTALL,
        /**
         * Send a report for a handled Java exception
         */
        NOTIFY_HANDLED,
        /**
         * Send a report for an unhandled error in the Java layer
         */
        NOTIFY_UNHANDLED,
        /**
         * Remove a metadata value. The Message object should be a string array
         * containing [tab, key]
         */
        REMOVE_METADATA,
        /**
         * A new session was started. The Message object should be a string
         * array
         * containing [id, startDateIsoString]
         */
        START_SESSION,

        /**
         * A session was stopped.
         */
        STOP_SESSION,

        /**
         * Set a new app version. The Message object should be the new app
         * version
         */
        UPDATE_APP_VERSION,
        /**
         * Set a new build UUID. The Message object should be the new build
         * UUID string
         */
        UPDATE_BUILD_UUID,
        /**
         * Set a new context. The message object should be the new context
         */
        UPDATE_CONTEXT,
        /**
         * Set a new value for `app.inForeground`. The message object should be a
         * List containing the values [inForeground (Boolean),
         * foregroundActivityName (String)]
         */
        UPDATE_IN_FOREGROUND,
        /**
         * Set a new value for `app.lowMemory`. The message object should be a
         * Boolean
         */
        UPDATE_LOW_MEMORY,
        /**
         * Set a new value for all custom metadata. The message object should be
         * MetaData.
         */
        UPDATE_METADATA,
        /**
         * Set a new value for `device.orientation`. The message object should
         * be the orientation in degrees
         */
        UPDATE_ORIENTATION,
        /**
         * Set a new value for `app.releaseStage`. The message object should be
         * the new release stage
         */
        UPDATE_RELEASE_STAGE,
        /**
         * Set a new value for user email. The message object is a string
         */
        UPDATE_USER_EMAIL,
        /**
         * Set a new value for user name. The message object is a string
         */
        UPDATE_USER_NAME,
        /**
         * Set a new value for user id. The message object is a string
         */
        UPDATE_USER_ID,
    }

    /**
     * Wrapper for messages sent to native observers
     */
    public static class Message {

        @NonNull
        public final MessageType type;

        @Nullable
        public final Object value;

        public Message(@NonNull MessageType type, @Nullable Object value) {
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

    /**
     * Caches a client instance for responding to future events
     */
    public static void setClient(@NonNull Client client) {
        if (NativeInterface.client == client) {
            return;
        }
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

    @Nullable
    public static String getContext() {
        return getClient().getContext();
    }

    public static boolean getLoggingEnabled() {
        return Logger.getEnabled();
    }

    @NonNull
    public static String getNativeReportPath() {
        return getClient().appContext.getCacheDir().getAbsolutePath() + "/bugsnag-native/";
    }

    /**
     * Retrieve user data from the static Client instance as a Map
     */
    @NonNull
    @SuppressWarnings("unused")
    public static Map<String,String> getUserData() {
        HashMap<String, String> userData = new HashMap<>();
        User user = getClient().getUser();
        userData.put("id", user.getId());
        userData.put("name", user.getName());
        userData.put("email", user.getEmail());

        return userData;
    }

    /**
     * Retrieve app data from the static Client instance as a Map
     */
    @NonNull
    @SuppressWarnings("unused")
    public static Map<String,Object> getAppData() {
        HashMap<String,Object> data = new HashMap<>();
        AppData source = getClient().getAppData();
        data.putAll(source.getAppData());
        data.putAll(source.getAppDataMetaData());
        return data;
    }

    /**
     * Retrieve device data from the static Client instance as a Map
     */
    @NonNull
    @SuppressWarnings("unused")
    public static Map<String,Object> getDeviceData() {
        HashMap<String,Object> deviceData = new HashMap<>();
        DeviceData source = getClient().getDeviceData();
        deviceData.putAll(source.getDeviceMetaData());
        deviceData.putAll(source.getDeviceData()); // wat
        return deviceData;
    }

    /**
     * Retrieve the CPU ABI(s) for the current device
     */
    @NonNull
    public static String[] getCpuAbi() {
        return getClient().deviceData.cpuAbi;
    }

    /**
     * Retrieves global metadata from the static Client instance as a Map
     */
    @NonNull
    public static Map<String, Object> getMetaData() {
        return new HashMap<>(getClient().getMetaData().store);
    }

    /**
     * Retrieves breadcrumbs from the static Client instance as a Map
     */
    @NonNull
    public static List<Breadcrumb> getBreadcrumbs() {
        Queue<Breadcrumb> store = getClient().breadcrumbs.store;
        return new ArrayList<>(store);
    }

    /**
     * Sets the user
     *
     * @param id id
     * @param email email
     * @param name name
     */
    @SuppressWarnings("unused")
    public static void setUser(@Nullable final String id,
                               @Nullable final String email,
                               @Nullable final String name) {
        Client client = getClient();
        client.setUserId(id);
        client.setUserEmail(email);
        client.setUserName(name);
    }

    /**
     * Leave a "breadcrumb" log message
     */
    public static void leaveBreadcrumb(@NonNull final String name,
                                       @NonNull final BreadcrumbType type) {
        getClient().leaveBreadcrumb(name, type, new HashMap<String, String>());
    }

    /**
     * Leaves a breadcrumb on the static client instance
     */
    public static void leaveBreadcrumb(@NonNull String name,
                                       @NonNull String type,
                                       @NonNull Map<String, String> metadata) {
        String typeName = type.toUpperCase(Locale.US);
        Map<String, String> map = metadata == null ? new HashMap<String, String>() : metadata;
        getClient().leaveBreadcrumb(name, BreadcrumbType.valueOf(typeName), map);
    }

    /**
     * Add metadata to subsequent exception reports
     */
    public static void addToTab(@NonNull final String tab,
                                @NonNull final String key,
                                @Nullable final Object value) {
        getClient().addToTab(tab, key, value);
    }

    /**
     * Set the client report release stage
     */
    public static void setReleaseStage(@Nullable final String stage) {
        getClient().setReleaseStage(stage);
    }

    /**
     * Return the client report release stage
     */
    @Nullable
    public static String getReleaseStage() {
        return getClient().getConfig().getReleaseStage();
    }

    /**
     * Return the client session endpoint
     */
    @NonNull
    public static String getSessionEndpoint() {
        return getClient().getConfig().getSessionEndpoint();
    }

    /**
     * Return the client report endpoint
     */
    @NonNull
    public static String getEndpoint() {
        return getClient().getConfig().getEndpoint();
    }

    /**
     * Set the client session endpoint
     */
    @SuppressWarnings("deprecation")
    public static void setSessionEndpoint(@NonNull final String endpoint) {
        getClient().getConfig().setSessionEndpoint(endpoint);
    }

    /**
     * Set the client report endpoint
     */
    @SuppressWarnings("deprecation")
    public static void setEndpoint(@NonNull final String endpoint) {
        getClient().getConfig().setEndpoint(endpoint);
    }

    /**
     * Set the client report context
     */
    public static void setContext(@Nullable final String context) {
        getClient().setContext(context);
    }

    /**
     * Set the client report app version
     */
    public static void setAppVersion(@NonNull final String version) {
        getClient().setAppVersion(version);
    }

    /**
     * Set the binary arch used in the application
     */
    public static void setBinaryArch(@NonNull final String binaryArch) {
        getClient().setBinaryArch(binaryArch);
    }

    /**
     * Return the client report app version
     */
    @NonNull
    public static String getAppVersion() {
        return getClient().getConfig().getAppVersion();
    }

    /**
     * Return which release stages notify
     */
    @Nullable
    public static String[] getNotifyReleaseStages() {
        return getClient().getConfig().getNotifyReleaseStages();
    }

    /**
     * Set which release stages notify
     */
    public static void setNotifyReleaseStages(@Nullable String[] notifyReleaseStages) {
        getClient().getConfig().setNotifyReleaseStages(notifyReleaseStages);
    }

    /**
     * Update the current session with a given start time, ID, and event counts
     */
    public static void registerSession(long startedAt, @Nullable String sessionId,
                                       int unhandledCount, int handledCount) {
        Client client = getClient();
        User user = client.getUser();
        Date startDate = startedAt > 0 ? new Date(startedAt) : null;
        client.getSessionTracker().registerExistingSession(startDate, sessionId, user,
                                                           unhandledCount, handledCount);
    }

    /**
     * Deliver a report, serialized as an event JSON payload.
     *
     * @param releaseStage The release stage in which the event was captured. Used to determin
     *                     whether the report should be discarded, based on configured release
     *                     stages
     */
    @SuppressWarnings("unused")
    public static void deliverReport(@Nullable String releaseStage, @NonNull String payload) {
        Client client = getClient();
        if (releaseStage == null
            || releaseStage.length() == 0
            || client.getConfig().shouldNotifyForReleaseStage(releaseStage)) {
            client.getErrorStore().enqueueContentForDelivery(payload);
            client.getErrorStore().flushAsync();
        }
    }

    /**
     * Notifies using the Android SDK
     *
     * @param name the error name
     * @param message the error message
     * @param severity the error severity
     * @param stacktrace a stacktrace
     */
    public static void notify(@NonNull final String name,
                              @NonNull final String message,
                              @NonNull final Severity severity,
                              @NonNull final StackTraceElement[] stacktrace) {

        getClient().notify(name, message, stacktrace, new Callback() {
            @Override
            public void beforeNotify(@NonNull Report report) {
                Error error = report.getError();
                if (error != null) {
                    if (severity != null) {
                        error.setSeverity(severity);
                    }
                    error.getExceptions().setExceptionType("c");
                }
            }
        });
    }
}
