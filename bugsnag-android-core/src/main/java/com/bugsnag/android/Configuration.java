package com.bugsnag.android;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * User-specified configuration storage object, contains information
 * specified at the client level, api-key and endpoint configuration.
 */
@SuppressWarnings("ConstantConditions") // suppress warning about making redundant null checks
public class Configuration implements CallbackAware, MetadataAware, UserAware {

    private static final int MIN_BREADCRUMBS = 0;
    private static final int MAX_BREADCRUMBS = 100;
    private static final String API_KEY_REGEX = "[A-Fa-f0-9]{32}";
    private static final long MIN_LAUNCH_CRASH_THRESHOLD_MS = 0;

    final ConfigInternal impl;

    /**
     * Constructs a new Configuration object with default values.
     */
    public Configuration(@NonNull String apiKey) {
        validateApiKey(apiKey);
        impl = new ConfigInternal(apiKey);
    }

    /**
     * Loads a Configuration object from values supplied as meta-data elements in your
     * AndroidManifest.
     */
    @NonNull
    public static Configuration load(@NonNull Context context) {
        return ConfigInternal.load(context);
    }

    @NonNull
    static Configuration load(@NonNull Context context, @NonNull String apiKey) {
        return ConfigInternal.load(context, apiKey);
    }

    private void validateApiKey(String value) {
        if (value == null || !value.matches(API_KEY_REGEX)) {
            throw new IllegalArgumentException("You must provide a valid Bugsnag API key");
        }
    }

    private void logNull(String property) {
        getLogger().e("Invalid null value supplied to config." + property + ", ignoring");
    }

    /**
     * Retrieves the API key used for events sent to Bugsnag.
     */
    @NonNull
    public String getApiKey() {
        return impl.getApiKey();
    }

    /**
     * Changes the API key used for events sent to Bugsnag.
     */
    public void setApiKey(@NonNull String apiKey) {
        validateApiKey(apiKey);
        impl.setApiKey(apiKey);
    }

    /**
     * Sets a unique identifier for the app build to be included in all events sent to Bugsnag.
     *
     * This is used to identify proguard
     * mapping files in the case that you publish multiple different apps with
     * the same appId and versionCode. The default value is read from the
     * com.bugsnag.android.BUILD_UUID meta-data field in your app manifest.
     */
    @Nullable
    public String getBuildUuid() {
        return impl.getBuildUuid();
    }

    /**
     * Sets a unique identifier for the app build to be included in all events sent to Bugsnag.
     *
     * This is used to identify proguard
     * mapping files in the case that you publish multiple different apps with
     * the same appId and versionCode. The default value is read from the
     * com.bugsnag.android.BUILD_UUID meta-data field in your app manifest.
     */
    public void setBuildUuid(@Nullable String buildUuid) {
        impl.setBuildUuid(buildUuid);
    }

    /**
     * Set the application version sent to Bugsnag. We'll automatically pull your app version
     * from the versionName field in your AndroidManifest.xml file.
     */
    @Nullable
    public String getAppVersion() {
        return impl.getAppVersion();
    }

    /**
     * Set the application version sent to Bugsnag. We'll automatically pull your app version
     * from the versionName field in your AndroidManifest.xml file.
     */
    public void setAppVersion(@Nullable String appVersion) {
        impl.setAppVersion(appVersion);
    }

    /**
     * We'll automatically pull your versionCode from the versionCode field
     * in your AndroidManifest.xml file. If you'd like to override this you
     * can set this property.
     */
    @Nullable
    public Integer getVersionCode() {
        return impl.getVersionCode();
    }

    /**
     * We'll automatically pull your versionCode from the versionCode field
     * in your AndroidManifest.xml file. If you'd like to override this you
     * can set this property.
     */
    public void setVersionCode(@Nullable Integer versionCode) {
        impl.setVersionCode(versionCode);
    }

    /**
     * If you would like to distinguish between errors that happen in different stages of the
     * application release process (development, production, etc) you can set the releaseStage
     * that is reported to Bugsnag.
     *
     * If you are running a debug build, we'll automatically set this to "development",
     * otherwise it is set to "production". You can control whether events are sent for
     * specific release stages using the enabledReleaseStages option.
     */
    @Nullable
    public String getReleaseStage() {
        return impl.getReleaseStage();
    }

    /**
     * If you would like to distinguish between errors that happen in different stages of the
     * application release process (development, production, etc) you can set the releaseStage
     * that is reported to Bugsnag.
     *
     * If you are running a debug build, we'll automatically set this to "development",
     * otherwise it is set to "production". You can control whether events are sent for
     * specific release stages using the enabledReleaseStages option.
     */
    public void setReleaseStage(@Nullable String releaseStage) {
        impl.setReleaseStage(releaseStage);
    }

    /**
     * Controls whether we should capture and serialize the state of all threads at the time
     * of an error.
     *
     * By default sendThreads is set to Thread.ThreadSendPolicy.ALWAYS. This can be set to
     * Thread.ThreadSendPolicy.NEVER to disable or Thread.ThreadSendPolicy.UNHANDLED_ONLY
     * to only do so for unhandled errors.
     */
    @NonNull
    public ThreadSendPolicy getSendThreads() {
        return impl.getSendThreads();
    }

    /**
     * Controls whether we should capture and serialize the state of all threads at the time
     * of an error.
     *
     * By default sendThreads is set to Thread.ThreadSendPolicy.ALWAYS. This can be set to
     * Thread.ThreadSendPolicy.NEVER to disable or Thread.ThreadSendPolicy.UNHANDLED_ONLY
     * to only do so for unhandled errors.
     */
    public void setSendThreads(@NonNull ThreadSendPolicy sendThreads) {
        if (sendThreads != null) {
            impl.setSendThreads(sendThreads);
        } else {
            logNull("sendThreads");
        }
    }

    /**
     * Set whether or not Bugsnag should persist user information between application sessions.
     *
     * If enabled then any user information set will be re-used until the user information is
     * removed manually by calling {@link Bugsnag#setUser(String, String, String)}
     * with null arguments.
     */
    public boolean getPersistUser() {
        return impl.getPersistUser();
    }

    /**
     * Set whether or not Bugsnag should persist user information between application sessions.
     *
     * If enabled then any user information set will be re-used until the user information is
     * removed manually by calling {@link Bugsnag#setUser(String, String, String)}
     * with null arguments.
     */
    public void setPersistUser(boolean persistUser) {
        impl.setPersistUser(persistUser);
    }

    /**
     * Sets the threshold in milliseconds for an uncaught error to be considered as a crash on
     * launch. If a crash is detected on launch, Bugsnag will attempt to send the event
     * synchronously.
     *
     * By default, this value is set at 5,000ms. Setting the value to 0 will disable this behaviour.
     */
    public long getLaunchCrashThresholdMs() {
        return impl.getLaunchCrashThresholdMs();
    }

    /**
     * Sets the threshold in milliseconds for an uncaught error to be considered as a crash on
     * launch. If a crash is detected on launch, Bugsnag will attempt to send the event
     * synchronously.
     *
     * By default, this value is set at 5,000ms. Setting the value to 0 will disable this behaviour.
     */
    public void setLaunchCrashThresholdMs(long launchCrashThresholdMs) {
        if (launchCrashThresholdMs > MIN_LAUNCH_CRASH_THRESHOLD_MS) {
            impl.setLaunchCrashThresholdMs(launchCrashThresholdMs);
        } else {
            getLogger().e(String.format(Locale.US, "Invalid configuration value detected. "
                    + "Option launchCrashThresholdMs should be a positive long value."
                    + "Supplied value is %d", launchCrashThresholdMs));
        }
    }

    /**
     * Sets whether or not Bugsnag should automatically capture and report User sessions whenever
     * the app enters the foreground.
     *
     * By default this behavior is enabled.
     */
    public boolean getAutoTrackSessions() {
        return impl.getAutoTrackSessions();
    }

    /**
     * Sets whether or not Bugsnag should automatically capture and report User sessions whenever
     * the app enters the foreground.
     *
     * By default this behavior is enabled.
     */
    public void setAutoTrackSessions(boolean autoTrackSessions) {
        impl.setAutoTrackSessions(autoTrackSessions);
    }

    /**
     * Bugsnag will automatically detect different types of error in your application.
     * If you wish to control exactly which types are enabled, set this property.
     */
    @NonNull
    public ErrorTypes getEnabledErrorTypes() {
        return impl.getEnabledErrorTypes();
    }

    /**
     * Bugsnag will automatically detect different types of error in your application.
     * If you wish to control exactly which types are enabled, set this property.
     */
    public void setEnabledErrorTypes(@NonNull ErrorTypes enabledErrorTypes) {
        if (enabledErrorTypes != null) {
            impl.setEnabledErrorTypes(enabledErrorTypes);
        } else {
            logNull("enabledErrorTypes");
        }
    }

    /**
     * If you want to disable automatic detection of all errors, you can set this property to false.
     * By default this property is true.
     *
     * Setting autoDetectErrors to false will disable all automatic errors, regardless of the
     * error types enabled by enabledErrorTypes
     */
    public boolean getAutoDetectErrors() {
        return impl.getAutoDetectErrors();
    }

    /**
     * If you want to disable automatic detection of all errors, you can set this property to false.
     * By default this property is true.
     *
     * Setting autoDetectErrors to false will disable all automatic errors, regardless of the
     * error types enabled by enabledErrorTypes
     */
    public void setAutoDetectErrors(boolean autoDetectErrors) {
        impl.setAutoDetectErrors(autoDetectErrors);
    }

    /**
     * Intended for internal use only - sets the code bundle id for React Native
     */
    @Nullable
    public String getCodeBundleId() {
        return impl.getCodeBundleId();
    }

    /**
     * Intended for internal use only - sets the code bundle id for React Native
     */
    public void setCodeBundleId(@Nullable String codeBundleId) {
        impl.setCodeBundleId(codeBundleId);
    }

    /**
     * If your app's codebase contains different entry-points/processes, but reports to a single
     * Bugsnag project, you might want to add information denoting the type of process the error
     * came from.
     *
     * This information can be used in the dashboard to filter errors and to determine whether
     * an error is limited to a subset of appTypes.
     *
     * By default, this value is set to 'android'.
     */
    @Nullable
    public String getAppType() {
        return impl.getAppType();
    }

    /**
     * If your app's codebase contains different entry-points/processes, but reports to a single
     * Bugsnag project, you might want to add information denoting the type of process the error
     * came from.
     *
     * This information can be used in the dashboard to filter errors and to determine whether
     * an error is limited to a subset of appTypes.
     *
     * By default, this value is set to 'android'.
     */
    public void setAppType(@Nullable String appType) {
        impl.setAppType(appType);
    }

    /**
     * By default, the notifier's log messages will be logged using android.util.Log
     * with a "Bugsnag" tag unless the releaseStage is "production".
     *
     * To override this behavior, an alternative instance can be provided that implements the
     * Logger interface.
     */
    @Nullable
    public Logger getLogger() {
        return impl.getLogger();
    }

    /**
     * By default, the notifier's log messages will be logged using android.util.Log
     * with a "Bugsnag" tag unless the releaseStage is "production".
     *
     * To override this behavior, an alternative instance can be provided that implements the
     * Logger interface.
     */
    public void setLogger(@Nullable Logger logger) {
        impl.setLogger(logger);
    }

    /**
     * The Delivery implementation used to make network calls to the Bugsnag
     * <a href="https://docs.bugsnag.com/api/error-reporting/">Error Reporting</a> and
     * <a href="https://docs.bugsnag.com/api/sessions/">Sessions API</a>.
     *
     * This may be useful if you have requirements such as certificate pinning and rotation,
     * which are not supported by the default implementation.
     *
     * To provide custom delivery functionality, create a class which implements the Delivery
     * interface. Please note that request bodies must match the structure specified in the
     * <a href="https://docs.bugsnag.com/api/error-reporting/">Error Reporting</a> and
     * <a href="https://docs.bugsnag.com/api/sessions/">Sessions API</a> documentation.
     *
     * You can use the return type from the deliver functions to control the strategy for
     * retrying the transmission at a later date.
     *
     * If DeliveryStatus.UNDELIVERED is returned, the notifier will automatically cache
     * the payload and trigger delivery later on. Otherwise, if either DeliveryStatus.DELIVERED
     * or DeliveryStatus.FAILURE is returned the notifier will removed any cached payload
     * and no further delivery will be attempted.
     */
    @NonNull
    public Delivery getDelivery() {
        return impl.getDelivery();
    }

    /**
     * The Delivery implementation used to make network calls to the Bugsnag
     * <a href="https://docs.bugsnag.com/api/error-reporting/">Error Reporting</a> and
     * <a href="https://docs.bugsnag.com/api/sessions/">Sessions API</a>.
     *
     * This may be useful if you have requirements such as certificate pinning and rotation,
     * which are not supported by the default implementation.
     *
     * To provide custom delivery functionality, create a class which implements the Delivery
     * interface. Please note that request bodies must match the structure specified in the
     * <a href="https://docs.bugsnag.com/api/error-reporting/">Error Reporting</a> and
     * <a href="https://docs.bugsnag.com/api/sessions/">Sessions API</a> documentation.
     *
     * You can use the return type from the deliver functions to control the strategy for
     * retrying the transmission at a later date.
     *
     * If DeliveryStatus.UNDELIVERED is returned, the notifier will automatically cache
     * the payload and trigger delivery later on. Otherwise, if either DeliveryStatus.DELIVERED
     * or DeliveryStatus.FAILURE is returned the notifier will removed any cached payload
     * and no further delivery will be attempted.
     */
    public void setDelivery(@NonNull Delivery delivery) {
        if (delivery != null) {
            impl.setDelivery(delivery);
        } else {
            logNull("delivery");
        }
    }

    /**
     * Set the endpoints to send data to. By default we'll send error reports to
     * https://notify.bugsnag.com, and sessions to https://sessions.bugsnag.com, but you can
     * override this if you are using Bugsnag Enterprise to point to your own Bugsnag endpoints.
     */
    @NonNull
    public EndpointConfiguration getEndpoints() {
        return impl.getEndpoints();
    }

    /**
     * Set the endpoints to send data to. By default we'll send error reports to
     * https://notify.bugsnag.com, and sessions to https://sessions.bugsnag.com, but you can
     * override this if you are using Bugsnag Enterprise to point to your own Bugsnag endpoints.
     */
    public void setEndpoints(@NonNull EndpointConfiguration endpoints) {
        if (endpoints != null) {
            impl.setEndpoints(endpoints);
        } else {
            logNull("endpoints");
        }
    }

    /**
     * Sets the maximum number of breadcrumbs which will be stored. Once the threshold is reached,
     * the oldest breadcrumbs will be deleted.
     *
     * By default, 25 breadcrumbs are stored: this can be amended up to a maximum of 100.
     */
    public int getMaxBreadcrumbs() {
        return impl.getMaxBreadcrumbs();
    }

    /**
     * Sets the maximum number of breadcrumbs which will be stored. Once the threshold is reached,
     * the oldest breadcrumbs will be deleted.
     *
     * By default, 25 breadcrumbs are stored: this can be amended up to a maximum of 100.
     */
    public void setMaxBreadcrumbs(int maxBreadcrumbs) {
        if (maxBreadcrumbs >= MIN_BREADCRUMBS && maxBreadcrumbs <= MAX_BREADCRUMBS) {
            impl.setMaxBreadcrumbs(maxBreadcrumbs);
        } else {
            getLogger().e(String.format(Locale.US, "Invalid configuration value detected. "
                    + "Option maxBreadcrumbs should be an integer between 0-100. "
                    + "Supplied value is %d", maxBreadcrumbs));
        }
    }

    /**
     * Bugsnag uses the concept of "contexts" to help display and group your errors. Contexts
     * represent what was happening in your application at the time an error occurs.
     *
     * In an android app the "context" is automatically set as the foreground Activity.
     * If you would like to set this value manually, you should alter this property.
     */
    @Nullable
    public String getContext() {
        return impl.getContext();
    }

    /**
     * Bugsnag uses the concept of "contexts" to help display and group your errors. Contexts
     * represent what was happening in your application at the time an error occurs.
     *
     * In an android app the "context" is automatically set as the foreground Activity.
     * If you would like to set this value manually, you should alter this property.
     */
    public void setContext(@Nullable String context) {
        impl.setContext(context);
    }

    /**
     * Sets which values should be removed from any Metadata objects before
     * sending them to Bugsnag. Use this if you want to ensure you don't send
     * sensitive data such as passwords, and credit card numbers to our
     * servers. Any keys which contain these strings will be filtered.
     *
     * By default, redactedKeys is set to "password"
     */
    @NonNull
    public Set<String> getRedactedKeys() {
        return impl.getRedactedKeys();
    }

    /**
     * Sets which values should be removed from any Metadata objects before
     * sending them to Bugsnag. Use this if you want to ensure you don't send
     * sensitive data such as passwords, and credit card numbers to our
     * servers. Any keys which contain these strings will be filtered.
     *
     * By default, redactedKeys is set to "password"
     */
    public void setRedactedKeys(@NonNull Set<String> redactedKeys) {
        if (CollectionUtils.containsNullElements(redactedKeys)) {
            logNull("redactedKeys");
        } else {
            impl.setRedactedKeys(redactedKeys);
        }
    }

    /**
     * Allows you to specify the fully-qualified name of error classes that will be discarded
     * before being sent to Bugsnag if they are detected. The notifier performs an exact
     * match against the canonical class name.
     */
    @NonNull
    public Set<String> getDiscardClasses() {
        return impl.getDiscardClasses();
    }

    /**
     * Allows you to specify the fully-qualified name of error classes that will be discarded
     * before being sent to Bugsnag if they are detected. The notifier performs an exact
     * match against the canonical class name.
     */
    public void setDiscardClasses(@NonNull Set<String> discardClasses) {
        if (CollectionUtils.containsNullElements(discardClasses)) {
            logNull("discardClasses");
        } else {
            impl.setDiscardClasses(discardClasses);
        }
    }

    /**
     * By default, Bugsnag will be notified of events that happen in any releaseStage.
     * If you would like to change which release stages notify Bugsnag you can set this property.
     */
    @Nullable
    public Set<String> getEnabledReleaseStages() {
        return impl.getEnabledReleaseStages();
    }

    /**
     * By default, Bugsnag will be notified of events that happen in any releaseStage.
     * If you would like to change which release stages notify Bugsnag you can set this property.
     */
    public void setEnabledReleaseStages(@Nullable Set<String> enabledReleaseStages) {
        impl.setEnabledReleaseStages(enabledReleaseStages);
    }

    /**
     * By default we will automatically add breadcrumbs for common application events such as
     * activity lifecycle events and system intents. To amend this behavior,
     * override the enabled breadcrumb types. All breadcrumbs can be disabled by providing an
     * empty set.
     *
     * The following breadcrumb types can be enabled:
     *
     * - Captured errors: left when an error event is sent to the Bugsnag API.
     * - Manual breadcrumbs: left via the Bugsnag.leaveBreadcrumb function.
     * - Navigation changes: left for Activity Lifecycle events to track the user's journey in
     * the app.
     * - State changes: state breadcrumbs are left for system broadcast events. For example:
     * battery warnings, airplane mode, etc.
     * - User interaction: left when the user performs certain system operations.
     */
    @Nullable
    public Set<BreadcrumbType> getEnabledBreadcrumbTypes() {
        return impl.getEnabledBreadcrumbTypes();
    }

    /**
     * By default we will automatically add breadcrumbs for common application events such as
     * activity lifecycle events and system intents. To amend this behavior,
     * override the enabled breadcrumb types. All breadcrumbs can be disabled by providing an
     * empty set.
     *
     * The following breadcrumb types can be enabled:
     *
     * - Captured errors: left when an error event is sent to the Bugsnag API.
     * - Manual breadcrumbs: left via the Bugsnag.leaveBreadcrumb function.
     * - Navigation changes: left for Activity Lifecycle events to track the user's journey in
     * the app.
     * - State changes: state breadcrumbs are left for system broadcast events. For example:
     * battery warnings, airplane mode, etc.
     * - User interaction: left when the user performs certain system operations.
     */
    public void setEnabledBreadcrumbTypes(@Nullable Set<BreadcrumbType> enabledBreadcrumbTypes) {
        impl.setEnabledBreadcrumbTypes(enabledBreadcrumbTypes);
    }

    /**
     * Sets which package names Bugsnag should consider as a part of the
     * running application. We mark stacktrace lines as in-project if they
     * originate from any of these packages and this allows us to improve
     * the visual display of the stacktrace on the dashboard.
     *
     * By default, projectPackages is set to be the package you called Bugsnag.start from.
     */
    @NonNull
    public Set<String> getProjectPackages() {
        return impl.getProjectPackages();
    }

    /**
     * Sets which package names Bugsnag should consider as a part of the
     * running application. We mark stacktrace lines as in-project if they
     * originate from any of these packages and this allows us to improve
     * the visual display of the stacktrace on the dashboard.
     *
     * By default, projectPackages is set to be the package you called Bugsnag.start from.
     */
    public void setProjectPackages(@NonNull Set<String> projectPackages) {
        if (CollectionUtils.containsNullElements(projectPackages)) {
            logNull("projectPackages");
        } else {
            impl.setProjectPackages(projectPackages);
        }
    }

    /**
     * Add a "on error" callback, to execute code at the point where an error report is
     * captured in Bugsnag.
     *
     * You can use this to add or modify information attached to an Event
     * before it is sent to your dashboard. You can also return
     * <code>false</code> from any callback to prevent delivery. "on error"
     * callbacks do not run before reports generated in the event
     * of immediate app termination from crashes in C/C++ code.
     *
     * For example:
     *
     * Bugsnag.addOnError(new OnErrorCallback() {
     * public boolean run(Event event) {
     * event.setSeverity(Severity.INFO);
     * return true;
     * }
     * })
     *
     * @param onError a callback to run before sending errors to Bugsnag
     * @see OnErrorCallback
     */
    @Override
    public void addOnError(@NonNull OnErrorCallback onError) {
        if (onError != null) {
            impl.addOnError(onError);
        } else {
            logNull("addOnError");
        }
    }

    /**
     * Removes a previously added "on error" callback
     * @param onError the callback to remove
     */
    @Override
    public void removeOnError(@NonNull OnErrorCallback onError) {
        if (onError != null) {
            impl.removeOnError(onError);
        } else {
            logNull("removeOnError");
        }
    }

    /**
     * Add an "on breadcrumb" callback, to execute code before every
     * breadcrumb captured by Bugsnag.
     *
     * You can use this to modify breadcrumbs before they are stored by Bugsnag.
     * You can also return <code>false</code> from any callback to ignore a breadcrumb.
     *
     * For example:
     *
     * Bugsnag.onBreadcrumb(new OnBreadcrumbCallback() {
     * public boolean run(Breadcrumb breadcrumb) {
     * return false; // ignore the breadcrumb
     * }
     * })
     *
     * @param onBreadcrumb a callback to run before a breadcrumb is captured
     * @see OnBreadcrumbCallback
     */
    @Override
    public void addOnBreadcrumb(@NonNull OnBreadcrumbCallback onBreadcrumb) {
        if (onBreadcrumb != null) {
            impl.addOnBreadcrumb(onBreadcrumb);
        } else {
            logNull("addOnBreadcrumb");
        }
    }

    /**
     * Removes a previously added "on breadcrumb" callback
     * @param onBreadcrumb the callback to remove
     */
    @Override
    public void removeOnBreadcrumb(@NonNull OnBreadcrumbCallback onBreadcrumb) {
        if (onBreadcrumb != null) {
            impl.removeOnBreadcrumb(onBreadcrumb);
        } else {
            logNull("removeOnBreadcrumb");
        }
    }

    /**
     * Add an "on session" callback, to execute code before every
     * session captured by Bugsnag.
     *
     * You can use this to modify sessions before they are stored by Bugsnag.
     * You can also return <code>false</code> from any callback to ignore a session.
     *
     * For example:
     *
     * Bugsnag.onSession(new OnSessionCallback() {
     * public boolean run(Session session) {
     * return false; // ignore the session
     * }
     * })
     *
     * @param onSession a callback to run before a session is captured
     * @see OnSessionCallback
     */
    @Override
    public void addOnSession(@NonNull OnSessionCallback onSession) {
        if (onSession != null) {
            impl.addOnSession(onSession);
        } else {
            logNull("addOnSession");
        }
    }

    /**
     * Removes a previously added "on session" callback
     * @param onSession the callback to remove
     */
    @Override
    public void removeOnSession(@NonNull OnSessionCallback onSession) {
        if (onSession != null) {
            impl.removeOnSession(onSession);
        } else {
            logNull("removeOnSession");
        }
    }

    /**
     * Adds a map of multiple metadata key-value pairs to the specified section.
     */
    @Override
    public void addMetadata(@NonNull String section, @NonNull Map<String, ?> value) {
        if (section != null && value != null) {
            impl.addMetadata(section, value);
        } else {
            logNull("addMetadata");
        }
    }

    /**
     * Adds the specified key and value in the specified section. The value can be of
     * any primitive type or a collection such as a map, set or array.
     */
    @Override
    public void addMetadata(@NonNull String section, @NonNull String key, @Nullable Object value) {
        if (section != null && key != null) {
            impl.addMetadata(section, key, value);
        } else {
            logNull("addMetadata");
        }
    }

    /**
     * Removes all the data from the specified section.
     */
    @Override
    public void clearMetadata(@NonNull String section) {
        if (section != null) {
            impl.clearMetadata(section);
        } else {
            logNull("clearMetadata");
        }
    }

    /**
     * Removes data with the specified key from the specified section.
     */
    @Override
    public void clearMetadata(@NonNull String section, @NonNull String key) {
        if (section != null && key != null) {
            impl.clearMetadata(section, key);
        } else {
            logNull("clearMetadata");
        }
    }

    /**
     * Returns a map of data in the specified section.
     */
    @Nullable
    @Override
    public Map<String, Object> getMetadata(@NonNull String section) {
        if (section != null) {
            return impl.getMetadata(section);
        } else {
            logNull("getMetadata");
            return null;
        }
    }

    /**
     * Returns the value of the specified key in the specified section.
     */
    @Nullable
    @Override
    public Object getMetadata(@NonNull String section, @NonNull String key) {
        if (section != null && key != null) {
            return impl.getMetadata(section, key);
        } else {
            logNull("getMetadata");
            return null;
        }
    }

    /**
     * Returns the currently set User information.
     */
    @NonNull
    @Override
    public User getUser() {
        return impl.getUser();
    }

    /**
     * Sets the user associated with the event.
     */
    @Override
    public void setUser(@Nullable String id, @Nullable String email, @Nullable String name) {
        impl.setUser(id, email, name);
    }

    /**
     * Adds a plugin which will be loaded when the bugsnag notifier is instantiated.
     */
    public void addPlugin(@NonNull Plugin plugin) {
        if (plugin != null) {
            impl.addPlugin(plugin);
        } else {
            logNull("addPlugin");
        }
    }

    Set<Plugin> getPlugins() {
        return impl.getPlugins();
    }
}
