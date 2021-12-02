package com.bugsnag.android;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
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
         * Turn on detection for C/C++ crashes
         */
        ENABLE_NATIVE_CRASH_REPORTING,
        /**
         * Turn off detection for C/C++ crashes
         */
        DISABLE_NATIVE_CRASH_REPORTING,
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
         * Set a new value for `app.notifyReleaseStages`. The message object should be
         * the Configuration object
         */
        UPDATE_NOTIFY_RELEASE_STAGES,
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

    // The default charset on Android is always UTF-8
    private static Charset UTF8Charset = Charset.defaultCharset();

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

    @Deprecated
    public static void configureClientObservers(@NonNull Client client) {
        setClient(client);
    }

    @Nullable
    public static String getContext() {
        return getClient().getContext();
    }

    public static boolean getLoggingEnabled() {
        return Logger.getEnabled();
    }

    /**
     * Retrieves the path used to store native reports
     */
    @NonNull
    public static String getNativeReportPath() {
        Configuration config = getClient().getConfig();
        File persistenceDirectory = config.getPersistenceDirectory();
        return new File(persistenceDirectory, "bugsnag-native").getAbsolutePath();
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
     * Sets the user
     *
     * @param idBytes id
     * @param emailBytes email
     * @param nameBytes name
     */
    @SuppressWarnings("unused")
    public static void setUser(@Nullable final byte[] idBytes,
                               @Nullable final byte[] emailBytes,
                               @Nullable final byte[] nameBytes) {
        String id = idBytes == null ? null : new String(idBytes, UTF8Charset);
        String email = emailBytes == null ? null : new String(emailBytes, UTF8Charset);
        String name = nameBytes == null ? null : new String(nameBytes, UTF8Charset);
        setUser(id, email, name);
    }

    /**
     * Leave a "breadcrumb" log message
     */
    public static void leaveBreadcrumb(@NonNull final String name,
                                       @NonNull final BreadcrumbType type) {
        if (name == null) {
            return;
        }
        getClient().leaveBreadcrumb(name, type, Collections.<String, String>emptyMap());
    }

    /**
     * Leave a "breadcrumb" log message
     */
    public static void leaveBreadcrumb(@NonNull final byte[] nameBytes,
                                       @NonNull final BreadcrumbType type) {
        if (nameBytes == null) {
            return;
        }
        String name = new String(nameBytes, UTF8Charset);
        getClient().leaveBreadcrumb(name, type, Collections.<String, String>emptyMap());
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
     * Remove metadata from subsequent exception reports
     */
    public static void clearTab(@NonNull final String tab) {
        getClient().clearTab(tab);
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
     * Enable automatic reporting of ANRs.
     */
    public static void enableAnrReporting() {
        getClient().enableAnrReporting();
    }

    /**
     * Disable automatic reporting of ANRs.
     */
    public static void disableAnrReporting() {
        getClient().disableAnrReporting();
    }

    /**
     * Enable automatic reporting of C/C++ crashes.
     */
    public static void enableNdkCrashReporting() {
        getClient().enableNdkCrashReporting();
    }

    /**
     * Disable automatic reporting of C/C++ crashes.
     */
    public static void disableNdkCrashReporting() {
        getClient().disableNdkCrashReporting();
    }

    /**
     * Enable automatic reporting of uncaught Java exceptions.
     */
    public static void enableUncaughtJavaExceptionReporting() {
        getClient().enableExceptionHandler();
    }

    /**
     * Disable automatic reporting of uncaught Java exceptions.
     */
    public static void disableUncaughtJavaExceptionReporting() {
        getClient().disableExceptionHandler();
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
     * @param releaseStageBytes The release stage in which the event was
     *                          captured. Used to determine whether the report
     *                          should be discarded, based on configured release
     *                          stages
     * @param payloadBytes The raw JSON payload of the event
     */
    @SuppressWarnings("unused")
    public static void deliverReport(@Nullable byte[] releaseStageBytes,
                                     @NonNull byte[] payloadBytes) {
        if (payloadBytes == null) {
            return;
        }
        String payload = new String(payloadBytes, UTF8Charset);
        String releaseStage = releaseStageBytes == null
                ? null
                : new String(releaseStageBytes, UTF8Charset);
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
     * @param nameBytes the error name
     * @param messageBytes the error message
     * @param severity the error severity
     * @param stacktrace a stacktrace
     */
    public static void notify(@NonNull final byte[] nameBytes,
                              @NonNull final byte[] messageBytes,
                              @NonNull final Severity severity,
                              @NonNull final StackTraceElement[] stacktrace) {
        if (nameBytes == null || messageBytes == null || stacktrace == null) {
            return;
        }
        String name = new String(nameBytes, UTF8Charset);
        String message = new String(messageBytes, UTF8Charset);
        notify(name, message, severity, stacktrace);
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
        if (name == null || message == null || stacktrace == null) {
            return;
        }
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
