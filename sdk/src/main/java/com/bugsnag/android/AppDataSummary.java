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

    @Nullable
    protected Integer versionCode;

    @Nullable
    protected String versionName;

    @NonNull
    private String releaseStage;

    @NonNull
    private String notifierType = "android";

    @Nullable
    private String codeBundleId;

    AppDataSummary(@NonNull Context appContext, @NonNull Configuration config) {
        versionCode = calculateVersionCode(appContext);

        codeBundleId = config.getCodeBundleId();
        String configType = config.getNotifierType();

        if (configType != null) {
            notifierType = configType;
        }

        if (config.getReleaseStage() != null) {
            releaseStage = config.getReleaseStage();
        } else {
            releaseStage = guessReleaseStage(appContext);
        }

        if (config.getAppVersion() != null) {
            versionName = config.getAppVersion();
        } else {
            versionName = calculateVersionName(appContext);
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
            .name("releaseStage").value(releaseStage)
            .name("version").value(versionName)
            .name("versionCode").value(versionCode)
            .name("codeBundleId").value(codeBundleId);
    }

    @Nullable
    public Integer getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(@Nullable Integer versionCode) {
        this.versionCode = versionCode;
    }

    @Nullable
    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(@Nullable String versionName) {
        this.versionName = versionName;
    }

    @NonNull
    public String getReleaseStage() {
        return releaseStage;
    }

    public void setReleaseStage(@NonNull String releaseStage) {
        this.releaseStage = releaseStage;
    }

    @NonNull
    public String getNotifierType() {
        return notifierType;
    }

    public void setNotifierType(@NonNull String notifierType) {
        this.notifierType = notifierType;
    }

    @Nullable
    public String getCodeBundleId() {
        return codeBundleId;
    }

    public void setCodeBundleId(@Nullable String codeBundleId) {
        this.codeBundleId = codeBundleId;
    }

    /**
     * The version code of the running Android app, from android:versionCode
     * in AndroidManifest.xml
     */
    @Nullable
    private static Integer calculateVersionCode(@NonNull Context appContext) {
        try {
            String packageName = appContext.getPackageName();
            return appContext.getPackageManager().getPackageInfo(packageName, 0).versionCode;
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
    private static String calculateVersionName(@NonNull Context appContext) {
        try {
            String packageName = appContext.getPackageName();
            return appContext.getPackageManager().getPackageInfo(packageName, 0).versionName;
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
            String packageName = appContext.getPackageName();
            PackageManager packageManager = appContext.getPackageManager();
            int appFlags = packageManager.getApplicationInfo(packageName, 0).flags;
            if ((appFlags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                return RELEASE_STAGE_DEVELOPMENT;
            }
        } catch (PackageManager.NameNotFoundException exception) {
            Logger.warn("Could not get releaseStage");
        }
        return RELEASE_STAGE_PRODUCTION;
    }

}
