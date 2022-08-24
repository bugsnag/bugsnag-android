package com.bugsnag.android;

import com.bugsnag.android.internal.ImmutableConfig;
import com.bugsnag.android.internal.JsonHelper;
import com.bugsnag.android.repackaged.dslplatform.json.DslJson;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Used as the entry point for native code to allow proguard to obfuscate other areas if needed
 */
public class NativeInterface {

    // The default charset on Android is always UTF-8
    private static Charset UTF8Charset = Charset.defaultCharset();

    private enum NativeApiCall {
        BSG_API_ADD_ON_ERROR,
        BSG_API_APP_GET_BINARY_ARCH,
        BSG_API_APP_GET_BUILD_UUID,
        BSG_API_APP_GET_DURATION,
        BSG_API_APP_GET_DURATION_IN_FOREGROUND,
        BSG_API_APP_GET_ID,
        BSG_API_APP_GET_IN_FOREGROUND,
        BSG_API_APP_GET_IS_LAUNCHING,
        BSG_API_APP_GET_RELEASE_STAGE,
        BSG_API_APP_GET_TYPE,
        BSG_API_APP_GET_VERSION,
        BSG_API_APP_GET_VERSION_CODE,
        BSG_API_APP_SET_BINARY_ARCH,
        BSG_API_APP_SET_BUILD_UUID,
        BSG_API_APP_SET_DURATION,
        BSG_API_APP_SET_DURATION_IN_FOREGROUND,
        BSG_API_APP_SET_ID,
        BSG_API_APP_SET_IN_FOREGROUND,
        BSG_API_APP_SET_IS_LAUNCHING,
        BSG_API_APP_SET_RELEASE_STAGE,
        BSG_API_APP_SET_TYPE,
        BSG_API_APP_SET_VERSION,
        BSG_API_APP_SET_VERSION_CODE,
        BSG_API_DEVICE_GET_ID,
        BSG_API_DEVICE_GET_JAILBROKEN,
        BSG_API_DEVICE_GET_LOCALE,
        BSG_API_DEVICE_GET_MANUFACTURER,
        BSG_API_DEVICE_GET_MODEL,
        BSG_API_DEVICE_GET_ORIENTATION,
        BSG_API_DEVICE_GET_OS_NAME,
        BSG_API_DEVICE_GET_OS_VERSION,
        BSG_API_DEVICE_GET_TIME,
        BSG_API_DEVICE_GET_TOTAL_MEMORY,
        BSG_API_DEVICE_SET_ID,
        BSG_API_DEVICE_SET_JAILBROKEN,
        BSG_API_DEVICE_SET_LOCALE,
        BSG_API_DEVICE_SET_MANUFACTURER,
        BSG_API_DEVICE_SET_MODEL,
        BSG_API_DEVICE_SET_ORIENTATION,
        BSG_API_DEVICE_SET_OS_NAME,
        BSG_API_DEVICE_SET_OS_VERSION,
        BSG_API_DEVICE_SET_TIME,
        BSG_API_DEVICE_SET_TOTAL_MEMORY,
        BSG_API_ERROR_GET_ERROR_CLASS,
        BSG_API_ERROR_GET_ERROR_MESSAGE,
        BSG_API_ERROR_GET_ERROR_TYPE,
        BSG_API_ERROR_SET_ERROR_CLASS,
        BSG_API_ERROR_SET_ERROR_MESSAGE,
        BSG_API_ERROR_SET_ERROR_TYPE,
        BSG_API_EVENT_ADD_METADATA_BOOL,
        BSG_API_EVENT_ADD_METADATA_DOUBLE,
        BSG_API_EVENT_ADD_METADATA_STRING,
        BSG_API_EVENT_CLEAR_METADATA,
        BSG_API_EVENT_CLEAR_METADATA_SECTION,
        BSG_API_EVENT_GET_API_KEY,
        BSG_API_EVENT_GET_CONTEXT,
        BSG_API_EVENT_GET_GROUPING_HASH,
        BSG_API_EVENT_GET_METADATA_BOOL,
        BSG_API_EVENT_GET_METADATA_DOUBLE,
        BSG_API_EVENT_GET_METADATA_STRING,
        BSG_API_EVENT_GET_SEVERITY,
        BSG_API_EVENT_GET_STACKFRAME,
        BSG_API_EVENT_GET_STACKTRACE_SIZE,
        BSG_API_EVENT_GET_USER,
        BSG_API_EVENT_HAS_METADATA,
        BSG_API_EVENT_IS_UNHANDLED,
        BSG_API_EVENT_SET_API_KEY,
        BSG_API_EVENT_SET_CONTEXT,
        BSG_API_EVENT_SET_GROUPING_HASH,
        BSG_API_EVENT_SET_SEVERITY,
        BSG_API_EVENT_SET_UNHANDLED,
        BSG_API_EVENT_SET_USER,
        END_MARKER
    }

    private static final String[] nativeApiCallNames = {
        "ndkOnError", // Specially named according to ROAD-1449 PD
        "app_get_binary_arch",
        "app_get_build_uuid",
        "app_get_duration",
        "app_get_duration_in_foreground",
        "app_get_id",
        "app_get_in_foreground",
        "app_get_is_launching",
        "app_get_release_stage",
        "app_get_type",
        "app_get_version",
        "app_get_version_code",
        "app_set_binary_arch",
        "app_set_build_uuid",
        "app_set_duration",
        "app_set_duration_in_foreground",
        "app_set_id",
        "app_set_in_foreground",
        "app_set_is_launching",
        "app_set_release_stage",
        "app_set_type",
        "app_set_version",
        "app_set_version_code",
        "device_get_id",
        "device_get_jailbroken",
        "device_get_locale",
        "device_get_manufacturer",
        "device_get_model",
        "device_get_orientation",
        "device_get_os_name",
        "device_get_os_version",
        "device_get_time",
        "device_get_total_memory",
        "device_set_id",
        "device_set_jailbroken",
        "device_set_locale",
        "device_set_manufacturer",
        "device_set_model",
        "device_set_orientation",
        "device_set_os_name",
        "device_set_os_version",
        "device_set_time",
        "device_set_total_memory",
        "error_get_error_class",
        "error_get_error_message",
        "error_get_error_type",
        "error_set_error_class",
        "error_set_error_message",
        "error_set_error_type",
        "event_add_metadata_bool",
        "event_add_metadata_double",
        "event_add_metadata_string",
        "event_clear_metadata",
        "event_clear_metadata_section",
        "event_get_api_key",
        "event_get_context",
        "event_get_grouping_hash",
        "event_get_metadata_bool",
        "event_get_metadata_double",
        "event_get_metadata_string",
        "event_get_severity",
        "event_get_stackframe",
        "event_get_stacktrace_size",
        "event_get_user",
        "event_has_metadata",
        "event_is_unhandled",
        "event_set_api_key",
        "event_set_context",
        "event_set_grouping_hash",
        "event_set_severity",
        "event_set_unhandled",
        "event_set_user",
    };

    /**
     * Static reference used if not using Bugsnag.start()
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
     * Create an empty Event for a "handled exception" report. The returned Event will have
     * no Error objects, metadata, breadcrumbs, or feature flags. It's indented that the caller
     * will populate the Error and then pass the Event object to
     * {@link Client#populateAndNotifyAndroidEvent(Event, OnErrorCallback)}.
     */
    private static Event createEmptyEvent() {
        Client client = getClient();

        return new Event(
                new EventInternal(
                        (Throwable) null,
                        client.getConfig(),
                        SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION),
                        client.getMetadataState().getMetadata().copy()
                ),
                client.getLogger()
        );
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

    /**
     * Retrieves the directory used to store native crash reports
     */
    @NonNull
    public static File getNativeReportPath() {
        return getNativeReportPath(getPersistenceDirectory());
    }

    private static @NonNull File getNativeReportPath(@NonNull File persistenceDirectory) {
        return new File(persistenceDirectory, "bugsnag-native");
    }

    private static @NonNull File getPersistenceDirectory() {
        return getClient().getConfig().getPersistenceDirectory().getValue();
    }

    private static @NonNull File getConfigDifferencesFilePath(@NonNull File persistenceDirectory) {
        return new File(persistenceDirectory, "bugsnag_last_run.config_differences.json");
    }

    static Map<String, Object> previousConfigDifferences;

    @SuppressWarnings("unchecked")
    static void loadPreviousConfigDifferences(@NonNull File persistenceDirectory) {
        File configDifferencesFile = getConfigDifferencesFilePath(persistenceDirectory);
        try {
            InputStream is = new FileInputStream(configDifferencesFile);
            DslJson<Map<String, Object>> dslJson = new DslJson<>();
            previousConfigDifferences = (Map<String, Object>)dslJson.deserialize(Map.class, is);
        } catch (FileNotFoundException exc) {
            // Ignore
        } catch (IOException exc) {
            // Ignore
        }
    }

    private static boolean isNdkCallIndexSet(@NonNull List<Long> bitfield, int index) {
        int element = index / 64;
        int bit = index & 63;
        return (bitfield.get(element) & (1L << bit)) != 0;
    }

    static @NonNull Map<String, Object> getNativeApiCallUsage() {
        List<Long> nativeApiUsage = getCalledNativeFunctions();
        Map<String, Object> callUsage = new HashMap<>();

        // Index 0 is a special case because it sets an integer instead of a
        // boolean to match the expectations of ROAD-1449 PD
        if (isNdkCallIndexSet(nativeApiUsage, 0)) {
            callUsage.put(nativeApiCallNames[0], 1L);
        }
        for (int i = 1; i < NativeApiCall.END_MARKER.ordinal(); i++) {
            if (isNdkCallIndexSet(nativeApiUsage, i)) {
                callUsage.put(nativeApiCallNames[i], true);
            }
        }
        return callUsage;
    }

    @SuppressWarnings("unchecked")
    private static @NonNull List<Long> getCalledNativeFunctions() {
        try {
            Class<Plugin> clz = (Class<Plugin>)Class.forName("com.bugsnag.android.NdkPlugin");
            Plugin ndkPlugin = client.getPlugin(clz);
            if (ndkPlugin != null) {
                Method method = ndkPlugin.getClass().getMethod("getCalledNativeFunctions");
                return (List<Long>)method.invoke(ndkPlugin);
            }
        } catch (Exception exc) {
            // Ignored
        }
        return new LinkedList<Long>() {{
                add(0L);
                add(0L);
            }
        };
    }

    /**
     * Persist config usage to disk
     * @param differences The differences to persist
     */
    public static void persistConfigDifferences(@NonNull File persistenceDirectory,
                                                @NonNull Map<String, Object> differences) {
        // Save the old config before overwriting it
        loadPreviousConfigDifferences(persistenceDirectory);

        Writer writer = null;
        JsonStream stream = null;
        try {
            File configDifferencesFile = getConfigDifferencesFilePath(persistenceDirectory);
            writer = new FileWriter(configDifferencesFile);
            stream = new JsonStream(writer);
            stream.value(differences);
        } catch (IOException exc) {
            // Ignore
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException exc) {
                    // Ignore
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException exc) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Retrieve user data from the static Client instance as a Map
     */
    @NonNull
    @SuppressWarnings("unused")
    public static Map<String, String> getUser() {
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
    public static Map<String, Object> getApp() {
        HashMap<String, Object> data = new HashMap<>();
        AppDataCollector source = getClient().getAppDataCollector();
        AppWithState app = source.generateAppWithState();
        data.put("version", app.getVersion());
        data.put("releaseStage", app.getReleaseStage());
        data.put("id", app.getId());
        data.put("type", app.getType());
        data.put("buildUUID", app.getBuildUuid());
        data.put("duration", app.getDuration());
        data.put("durationInForeground", app.getDurationInForeground());
        data.put("versionCode", app.getVersionCode());
        data.put("inForeground", app.getInForeground());
        data.put("isLaunching", app.isLaunching());
        data.put("binaryArch", app.getBinaryArch());
        data.putAll(source.getAppDataMetadata());
        return data;
    }

    /**
     * Retrieve device data from the static Client instance as a Map
     */
    @NonNull
    @SuppressWarnings("unused")
    public static Map<String, Object> getDevice() {
        DeviceDataCollector source = getClient().getDeviceDataCollector();
        HashMap<String, Object> deviceData = new HashMap<>(source.getDeviceMetadata());

        DeviceWithState src = source.generateDeviceWithState(new Date().getTime());
        deviceData.put("freeDisk", src.getFreeDisk());
        deviceData.put("freeMemory", src.getFreeMemory());
        deviceData.put("orientation", src.getOrientation());
        deviceData.put("time", src.getTime());
        deviceData.put("cpuAbi", src.getCpuAbi());
        deviceData.put("jailbroken", src.getJailbroken());
        deviceData.put("id", src.getId());
        deviceData.put("locale", src.getLocale());
        deviceData.put("manufacturer", src.getManufacturer());
        deviceData.put("model", src.getModel());
        deviceData.put("osName", src.getOsName());
        deviceData.put("osVersion", src.getOsVersion());
        deviceData.put("runtimeVersions", src.getRuntimeVersions());
        deviceData.put("totalMemory", src.getTotalMemory());
        return deviceData;
    }

    /**
     * Retrieve the CPU ABI(s) for the current device
     */
    @NonNull
    public static String[] getCpuAbi() {
        return getClient().getDeviceDataCollector().getCpuAbi();
    }

    /**
     * Retrieves global metadata from the static Client instance as a Map
     */
    @NonNull
    public static Map<String, Object> getMetadata() {
        return getClient().getMetadata();
    }

    /**
     * Retrieves a list of stored breadcrumbs from the static Client instance
     */
    @NonNull
    public static List<Breadcrumb> getBreadcrumbs() {
        return getClient().getBreadcrumbs();
    }

    /**
     * Sets the user
     *
     * @param id    id
     * @param email email
     * @param name  name
     */
    @SuppressWarnings("unused")
    public static void setUser(@Nullable final String id,
                               @Nullable final String email,
                               @Nullable final String name) {
        Client client = getClient();
        client.setUser(id, email, name);
    }

    /**
     * Sets the user
     *
     * @param idBytes    id
     * @param emailBytes email
     * @param nameBytes  name
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
        getClient().leaveBreadcrumb(name, new HashMap<String, Object>(), type);
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
        getClient().leaveBreadcrumb(name, new HashMap<String, Object>(), type);
    }

    /**
     * Leaves a breadcrumb on the static client instance
     */
    public static void leaveBreadcrumb(@NonNull String message,
                                       @NonNull String type,
                                       @NonNull Map<String, Object> metadata) {
        String typeName = type.toUpperCase(Locale.US);
        getClient().leaveBreadcrumb(message, metadata, BreadcrumbType.valueOf(typeName));
    }

    /**
     * Remove metadata from subsequent exception reports
     */
    public static void clearMetadata(@NonNull String section, @Nullable String key) {
        if (key == null) {
            getClient().clearMetadata(section);
        } else {
            getClient().clearMetadata(section, key);
        }
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
        return getClient().getConfig().getReleaseStage();
    }

    /**
     * Return the client session endpoint
     */
    @NonNull
    public static String getSessionEndpoint() {
        return getClient().getConfig().getEndpoints().getSessions();
    }

    /**
     * Return the client report endpoint
     */
    @NonNull
    public static String getEndpoint() {
        return getClient().getConfig().getEndpoints().getNotify();
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
    @Nullable
    public static String getAppVersion() {
        return getClient().getConfig().getAppVersion();
    }

    /**
     * Return which release stages notify
     */
    @Nullable
    public static Collection<String> getEnabledReleaseStages() {
        return getClient().getConfig().getEnabledReleaseStages();
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
     * Ask if an error class is on the configurable discard list.
     * This is used by the native layer to decide whether to pass an event to
     * deliverReport() or not.
     *
     * @param name The error class to ask about.
     */
    @SuppressWarnings("unused")
    public static boolean isDiscardErrorClass(@NonNull String name) {
        return getClient().getConfig().getDiscardClasses().contains(name);
    }

    /**
     * Deliver a report, serialized as an event JSON payload.
     *
     * @param releaseStageBytes The release stage in which the event was
     *                          captured. Used to determine whether the report
     *                          should be discarded, based on configured release
     *                          stages
     * @param payloadBytes      The raw JSON payload of the event
     * @param apiKey            The apiKey for the event
     * @param isLaunching       whether the crash occurred when the app was launching
     */
    @SuppressWarnings("unused")
    public static void deliverReport(@Nullable byte[] releaseStageBytes,
                                     @NonNull byte[] payloadBytes,
                                     @NonNull String apiKey,
                                     boolean isLaunching) {
        // Since this is a native event, add the previous config usage to it.
        if (previousConfigDifferences != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = (Map<String, Object>) JsonHelper.INSTANCE.deserialize(
                    new ByteArrayInputStream(payloadBytes));
            payloadMap.put("config", previousConfigDifferences);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            JsonHelper.INSTANCE.serialize(payloadMap, os);
            payloadBytes = os.toByteArray();
        }

        String payload = new String(payloadBytes, UTF8Charset);
        String releaseStage = releaseStageBytes == null
                ? null
                : new String(releaseStageBytes, UTF8Charset);
        Client client = getClient();
        ImmutableConfig config = client.getConfig();
        if (releaseStage == null
                || releaseStage.length() == 0
                || !config.shouldDiscardByReleaseStage()) {
            EventStore eventStore = client.getEventStore();

            String filename = eventStore.getNdkFilename(payload, apiKey);
            if (isLaunching) {
                filename = filename.replace(".json", "startupcrash.json");
            }
            eventStore.enqueueContentForDelivery(payload, filename);
        }
    }

    /**
     * Notifies using the Android SDK
     *
     * @param nameBytes    the error name
     * @param messageBytes the error message
     * @param severity     the error severity
     * @param stacktrace   a stacktrace
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
     * @param name       the error name
     * @param message    the error message
     * @param severity   the error severity
     * @param stacktrace a stacktrace
     */
    public static void notify(@NonNull final String name,
                              @NonNull final String message,
                              @NonNull final Severity severity,
                              @NonNull final StackTraceElement[] stacktrace) {
        if (getClient().getConfig().shouldDiscardError(name)) {
            return;
        }
        Throwable exc = new RuntimeException();
        exc.setStackTrace(stacktrace);

        getClient().notify(exc, new OnErrorCallback() {
            @Override
            public boolean onError(@NonNull Event event) {
                event.updateSeverityInternal(severity);
                List<Error> errors = event.getErrors();
                Error error = event.getErrors().get(0);

                // update the error's type to C
                if (!errors.isEmpty()) {
                    error.setErrorClass(name);
                    error.setErrorMessage(message);

                    for (Error err : errors) {
                        err.setType(ErrorType.C);
                    }
                }
                return true;
            }
        });
    }

    /**
     * Notifies using the Android SDK
     *
     * @param nameBytes    the error name
     * @param messageBytes the error message
     * @param severity     the error severity
     * @param stacktrace   a stacktrace
     */
    public static void notify(@NonNull final byte[] nameBytes,
                              @NonNull final byte[] messageBytes,
                              @NonNull final Severity severity,
                              @NonNull final NativeStackframe[] stacktrace) {

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
     * @param name       the error name
     * @param message    the error message
     * @param severity   the error severity
     * @param stacktrace a stacktrace
     */
    public static void notify(@NonNull final String name,
                              @NonNull final String message,
                              @NonNull final Severity severity,
                              @NonNull final NativeStackframe[] stacktrace) {
        Client client = getClient();

        if (client.getConfig().shouldDiscardError(name)) {
            return;
        }

        Event event = createEmptyEvent();
        event.updateSeverityInternal(severity);

        List<Stackframe> stackframes = new ArrayList<>(stacktrace.length);
        for (NativeStackframe nativeStackframe : stacktrace) {
            stackframes.add(new Stackframe(nativeStackframe));
        }
        event.getErrors().add(new Error(
                new ErrorInternal(name, message, new Stacktrace(stackframes), ErrorType.C),
                client.getLogger()
        ));

        getClient().populateAndNotifyAndroidEvent(event, null);
    }

    /**
     * Create an {@code Event} object
     *
     * @param exc            the Throwable object that caused the event
     * @param client         the Client object that the event is associated with
     * @param severityReason the severity of the Event
     * @return a new {@code Event} object
     */
    @NonNull
    public static Event createEvent(@Nullable Throwable exc,
                                    @NonNull Client client,
                                    @NonNull SeverityReason severityReason) {
        Metadata metadata = client.getMetadataState().getMetadata();
        FeatureFlags featureFlags = client.getFeatureFlagState().getFeatureFlags();
        return new Event(exc, client.getConfig(), severityReason, metadata, featureFlags,
                client.logger);
    }

    @NonNull
    public static Logger getLogger() {
        return getClient().getLogger();
    }

    /**
     * Switches automatic error detection on/off after Bugsnag has initialized.
     * This is required to support legacy functionality in Unity.
     *
     * @param autoNotify whether errors should be automatically detected.
     */
    public static void setAutoNotify(boolean autoNotify) {
        getClient().setAutoNotify(autoNotify);
    }

    /**
     * Switches automatic ANR detection on/off after Bugsnag has initialized.
     * This is required to support legacy functionality in Unity.
     *
     * @param autoDetectAnrs whether ANRs should be automatically detected.
     */
    public static void setAutoDetectAnrs(boolean autoDetectAnrs) {
        getClient().setAutoDetectAnrs(autoDetectAnrs);
    }

    public static void startSession() {
        getClient().startSession();
    }

    public static void pauseSession() {
        getClient().pauseSession();
    }

    public static boolean resumeSession() {
        return getClient().resumeSession();
    }

    @Nullable
    public static Session getCurrentSession() {
        return getClient().sessionTracker.getCurrentSession();
    }

    /**
     * Marks the launch period as complete
     */
    public static void markLaunchCompleted() {
        getClient().markLaunchCompleted();
    }

    /**
     * Get the last run info object
     */
    @Nullable
    public static LastRunInfo getLastRunInfo() {
        return getClient().getLastRunInfo();
    }

}
