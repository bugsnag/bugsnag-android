package com.bugsnag.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * Information about the running Android app which doesn't change over time,
 * including app name, version and release stage.
 * <p/>
 * App information in this class is cached during construction for faster
 * subsequent lookups and to reduce GC overhead.
 */
class AppDataSummary implements JsonStream.Streamable {

    static final String RELEASE_STAGE_DEVELOPMENT = "development";
    static final String RELEASE_STAGE_PRODUCTION = "production";

    @NonNull
    protected final Configuration config;

    @Nullable
    protected final Integer versionCode;

    @Nullable
    protected final String versionName;

    @NonNull
    private final String guessedReleaseStage;

    @Nullable
    private String notifierType = "android";

    @Nullable
    private String codeBundleId;

    AppDataSummary(@NonNull Context appContext, @NonNull Configuration config) {
        versionCode = getVersionCode(appContext);
        versionName = getVersionName(appContext);
        guessedReleaseStage = guessReleaseStage(appContext);
        this.config = config;

        codeBundleId = config.getCodeBundleId();
        String configType = config.getNotifierType();

        if (configType != null) {
            notifierType = configType;
        }
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        serialiseMinimalAppData(writer);
        writer.endObject();
    }

    void serialiseMinimalAppData(@NonNull JsonStream writer) throws IOException {
        writer
            .name("type").value(notifierType)
            .name("releaseStage").value(getReleaseStage())
            .name("version").value(getAppVersion())
            .name("versionCode").value(versionCode)
            .name("codeBundleId").value(codeBundleId);
    }

    @NonNull
    String getReleaseStage() {
        if (config.getReleaseStage() != null) {
            return config.getReleaseStage();
        } else {
            return guessedReleaseStage;
        }
    }

    @Nullable
    String getAppVersion() {
        if (config.getAppVersion() != null) {
            return config.getAppVersion();
        } else {
            return versionName;
        }
    }

    /**
     * The version code of the running Android app, from android:versionCode
     * in AndroidManifest.xml
     */
    @Nullable
    private static Integer getVersionCode(@NonNull Context appContext) {
        try {
            return appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException exception) {
            Logger.warn("Could not get versionCode");
        }
        return null;
    }

    /**
     * The version code of the running Android app, from android:versionName
     * in AndroidManifest.xml
     */
    @Nullable
    private static String getVersionName(@NonNull Context appContext) {
        try {
            return appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException exception) {
            Logger.warn("Could not get versionName");
        }
        return null;
    }

    /**
     * Guess the release stage of the running Android app by checking the
     * android:debuggable flag from AndroidManifest.xml
     */
    @NonNull
    static String guessReleaseStage(@NonNull Context appContext) {
        try {
            int appFlags = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), 0).flags;
            if ((appFlags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                return RELEASE_STAGE_DEVELOPMENT;
            }
        } catch (PackageManager.NameNotFoundException exception) {
            Logger.warn("Could not get releaseStage");
        }
        return RELEASE_STAGE_PRODUCTION;
    }

}
