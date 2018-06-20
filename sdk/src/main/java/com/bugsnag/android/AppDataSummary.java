package com.bugsnag.android;

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
public class AppDataSummary implements JsonStream.Streamable {

    @Nullable
    private Integer versionCode;

    @Nullable
    private String versionName;

    @NonNull
    private String releaseStage;

    @NonNull
    private String notifierType;

    @Nullable
    private String codeBundleId;

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

    /**
     * @return the application's version code
     */
    @Nullable
    public Integer getVersionCode() {
        return versionCode;
    }

    /**
     * Overrides the application's default version code
     *
     * @param versionCode the version code
     */
    public void setVersionCode(@Nullable Integer versionCode) {
        this.versionCode = versionCode;
    }

    /**
     * @return the application's version name
     */
    @Nullable
    public String getVersionName() {
        return versionName;
    }

    /**
     * Overrides the application's default version name
     *
     * @param versionName the version name
     */
    public void setVersionName(@Nullable String versionName) {
        this.versionName = versionName;
    }

    /**
     * @return the application's release stage
     */
    @NonNull
    public String getReleaseStage() {
        return releaseStage;
    }

    /**
     * Overrides the application's default release stage
     *
     * @param releaseStage the release stage
     */
    public void setReleaseStage(@NonNull String releaseStage) {
        this.releaseStage = releaseStage;
    }

    @NonNull
    @InternalApi
    public String getNotifierType() {
        return notifierType;
    }

    @InternalApi
    public void setNotifierType(@NonNull String notifierType) {
        this.notifierType = notifierType;
    }

    /**
     * @return the application's code bundle ID, if it exists
     */
    @Nullable
    public String getCodeBundleId() {
        return codeBundleId;
    }

    /**
     * Overrides the application's default code bundle ID
     *
     * @param codeBundleId the code bundle ID
     */
    public void setCodeBundleId(@Nullable String codeBundleId) {
        this.codeBundleId = codeBundleId;
    }
}
