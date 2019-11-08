package com.bugsnag.android;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
         * Clear all breadcrumbState
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
         * A session was paused.
         */
        PAUSE_SESSION,

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
         * Set a new value for `device.orientation`. The message object should
         * be the orientation in degrees
         */
        UPDATE_ORIENTATION,
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
        NativeInterface.client = client;
    }

    @Nullable
    public static String getContext() {
        return getClient().getContext();
    }

    @NonNull
    public static String getNativeReportPath() {
        return getClient().getAppContext().getCacheDir().getAbsolutePath() + "/bugsnag-native/";
    }

    /**
     * Retrieve user data from the static Client instance as a Map
     */
    @NonNull
    @SuppressWarnings("unused")
    public static Map<String,String> getUser() {
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
    public static Map<String,Object> getApp() {
        HashMap<String,Object> data = new HashMap<>();
        AppData source = getClient().getAppData();
        data.putAll(source.getAppData());
        data.putAll(source.getAppDataMetadata());
        return data;
    }

    /**
     * Retrieve device data from the static Client instance as a Map
     */
    @NonNull
    @SuppressWarnings("unused")
    public static Map<String,Object> getDevice() {
        HashMap<String,Object> deviceData = new HashMap<>();
        DeviceData source = getClient().getDeviceData();
        deviceData.putAll(source.getDeviceMetadata());
        deviceData.putAll(source.getDeviceData()); // wat
        return deviceData;
    }

    /**
     * Retrieve the CPU ABI(s) for the current device
     */
    @NonNull
    public static String[] getCpuAbi() {
        return getClient().getDeviceData().getCpuAbi();
    }

    /**
     * Retrieves global metadata from the static Client instance as a Map
     */
    @NonNull
    public static Map<String, Object> getMetadata() {
        return new HashMap<>(getClient().getMetadataState().getMetadata().toMap());
    }

    /**
     * Retrieves breadcrumbState from the static Client instance as a Map
     */
    @NonNull
    public static List<Breadcrumb> getBreadcrumbs() {
        Queue<Breadcrumb> store = getClient().getBreadcrumbState().getStore();
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
        getClient().leaveBreadcrumb(name, type, new HashMap<String, Object>());
    }

    /**
     * Leaves a breadcrumb on the static client instance
     */
    public static void leaveBreadcrumb(@NonNull String message,
                                       @NonNull String type,
                                       @NonNull Map<String, Object> metadata) {
        String typeName = type.toUpperCase(Locale.US);
        Map<String, Object> map = metadata == null ? new HashMap<String, Object>() : metadata;
        getClient().leaveBreadcrumb(message, BreadcrumbType.valueOf(typeName), map);
    }

    /**
     * Remove metadata from subsequent exception reports
     */
    public static void clearMetadata(@NonNull String section, @Nullable String key) {
        getClient().clearMetadata(section, key);
    }

    /**
     * Add metadata to subsequent exception reports
     */
    public static void addMetadata(@NonNull final String tab,
                                  @Nullable final String key,
                                  @Nullable final Object value) {
        getClient().addMetadata(tab, key, value);
    }

    /**
     * Return the client report release stage
     */
    @Nullable
    public static String getReleaseStage() {
        return getClient().getImmutableConfig().getReleaseStage();
    }

    /**
     * Return the client session endpoint
     */
    @NonNull
    public static String getSessionEndpoint() {
        return getClient().getImmutableConfig().getEndpoints().getSessions();
    }

    /**
     * Return the client report endpoint
     */
    @NonNull
    public static String getEndpoint() {
        return getClient().getImmutableConfig().getEndpoints().getNotify();
    }

    /**
     * Set the client report context
     */
    public static void setContext(@Nullable final String context) {
        getClient().setContext(context);
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
        return getClient().getImmutableConfig().getAppVersion();
    }

    /**
     * Return which release stages notify
     */
    @Nullable
    public static Collection<String> getEnabledReleaseStages() {
        return getClient().getImmutableConfig().getEnabledReleaseStages();
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
            || client.getImmutableConfig().shouldNotifyForReleaseStage()) {
            client.getEventStore().enqueueContentForDelivery(payload);
            client.getEventStore().flushAsync();
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

        getClient().notify(name, message, stacktrace, new OnError() {
            @Override
            public boolean run(@NonNull Event event) {
                event.updateSeverityInternal(severity);

                for (Error error : event.getErrors()) {
                    error.setType("c");
                }
                return true;
            }
        });
    }

    @NonNull
    public static Logger getLogger() {
        return getClient().getImmutableConfig().getLogger();
    }
}
