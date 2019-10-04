package com.bugsnag.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;

class ConfigFactory {

    private static final String BUGSNAG_NAMESPACE = "com.bugsnag.android";
    private static final String MF_APP_VERSION = BUGSNAG_NAMESPACE + ".APP_VERSION";
    private static final String MF_VERSION_CODE = BUGSNAG_NAMESPACE + ".VERSION_CODE";
    private static final String MF_ENDPOINT = BUGSNAG_NAMESPACE + ".ENDPOINT";
    private static final String MF_SESSIONS_ENDPOINT = BUGSNAG_NAMESPACE + ".SESSIONS_ENDPOINT";
    private static final String MF_RELEASE_STAGE = BUGSNAG_NAMESPACE + ".RELEASE_STAGE";
    private static final String MF_SEND_THREADS = BUGSNAG_NAMESPACE + ".SEND_THREADS";
    private static final String MF_ENABLE_EXCEPTION_HANDLER =
        BUGSNAG_NAMESPACE + ".ENABLE_EXCEPTION_HANDLER";
    private static final String MF_PERSIST_USER_BETWEEN_SESSIONS =
        BUGSNAG_NAMESPACE + ".PERSIST_USER_BETWEEN_SESSIONS";
    private static final String MF_AUTO_CAPTURE_SESSIONS =
        BUGSNAG_NAMESPACE + ".AUTO_CAPTURE_SESSIONS";
    private static final String MF_API_KEY = BUGSNAG_NAMESPACE + ".API_KEY";
    private static final String MF_DETECT_NDK_CRASHES = BUGSNAG_NAMESPACE + ".DETECT_NDK_CRASHES";
    private static final String MF_DETECT_ANRS = BUGSNAG_NAMESPACE + ".DETECT_ANRS";
    static final String MF_BUILD_UUID = BUGSNAG_NAMESPACE + ".BUILD_UUID";

    /**
     * Creates a new configuration object based on the provided parameters
     * will read the API key and other configuration values from the manifest if it is not provided
     *
     * @param androidContext         The context of the application
     * @param apiKey                 The API key to use
     * @param enableExceptionHandler should we automatically handle uncaught exceptions?
     * @return The created config
     */
    @NonNull
    static Configuration createNewConfiguration(@NonNull Context androidContext,
                                                String apiKey,
                                                boolean enableExceptionHandler) {
        Context appContext = androidContext.getApplicationContext();

        // Attempt to load API key and other config from AndroidManifest.xml, if not passed in
        boolean loadFromManifest = TextUtils.isEmpty(apiKey);

        if (loadFromManifest) {
            try {
                PackageManager packageManager = appContext.getPackageManager();
                String packageName = appContext.getPackageName();
                ApplicationInfo ai =
                    packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                Bundle data = ai.metaData;
                apiKey = data.getString(MF_API_KEY);
            } catch (Exception ignore) {
                Logger.warn("Bugsnag is unable to read api key from manifest.");
            }
        }

        if (apiKey == null) {
            throw new NullPointerException("You must provide a Bugsnag API key");
        }

        // Build a configuration object
        Configuration newConfig = new Configuration(apiKey);
        newConfig.setEnableExceptionHandler(enableExceptionHandler);

        if (loadFromManifest) {
            try {
                PackageManager packageManager = appContext.getPackageManager();
                String packageName = appContext.getPackageName();
                ApplicationInfo ai =
                    packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                Bundle data = ai.metaData;
                populateConfigFromManifest(newConfig, data);
            } catch (Exception ignore) {
                Logger.warn("Bugsnag is unable to read config from manifest.");
            }
        }
        return newConfig;
    }

    /**
     * Populates the config with meta-data values supplied from the manifest as a Bundle.
     *
     * @param config the config to mutate
     * @param data   the manifest bundle
     */
    static void populateConfigFromManifest(@NonNull Configuration config,
                                           @NonNull Bundle data) {
        config.setBuildUUID(data.getString(MF_BUILD_UUID));
        config.setAppVersion(data.getString(MF_APP_VERSION));
        config.setReleaseStage(data.getString(MF_RELEASE_STAGE));

        if (data.containsKey(MF_VERSION_CODE)) {
            config.setVersionCode(data.getInt(MF_VERSION_CODE));
        }
        if (data.containsKey(MF_ENDPOINT)) {
            String endpoint = data.getString(MF_ENDPOINT);
            String sessionEndpoint = data.getString(MF_SESSIONS_ENDPOINT);
            //noinspection ConstantConditions (pass in null/empty as this function will warn)
            config.setEndpoints(endpoint, sessionEndpoint);
        }

        config.setSendThreads(data.getBoolean(MF_SEND_THREADS, true));
        config.setPersistUserBetweenSessions(
            data.getBoolean(MF_PERSIST_USER_BETWEEN_SESSIONS, false));

        if (data.containsKey(MF_DETECT_NDK_CRASHES)) {
            config.setDetectNdkCrashes(data.getBoolean(MF_DETECT_NDK_CRASHES));
        }
        if (data.containsKey(MF_DETECT_ANRS)) {
            config.setDetectAnrs(data.getBoolean(MF_DETECT_ANRS));
        }
        if (data.containsKey(MF_AUTO_CAPTURE_SESSIONS)) {
            config.setAutoCaptureSessions(data.getBoolean(MF_AUTO_CAPTURE_SESSIONS));
        }

        config.setEnableExceptionHandler(
            data.getBoolean(MF_ENABLE_EXCEPTION_HANDLER, true));
    }

}
