package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * Information about the running Android app, including app name, version and release stage.
 */
public class AppData extends AppDataSummary {

    @SuppressWarnings("NullableProblems") // initialised after construction
    @NonNull
    private String packageName;

    @Nullable
    private String buildUuid;

    private long durationMs;
    private long foregroundMs;
    private boolean inForeground;

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        serialiseMinimalAppData(writer);
        writer.name("id").value(packageName);
        writer.name("buildUUID").value(buildUuid);
        writer.name("duration").value(durationMs);
        writer.name("durationInForeground").value(foregroundMs);
        writer.name("inForeground").value(inForeground);
        writer.endObject();
    }

    /**
     * @return the application's package name
     */
    @NonNull
    public String getPackageName() {
        return packageName;
    }

    /**
     * Overrides the application's default package name
     *
     * @param packageName the package name
     */
    public void setPackageName(@NonNull String packageName) {
        this.packageName = packageName;
    }

    /**
     * @return the application's bugsnag build UUID
     */
    @Nullable
    public String getBuildUuid() {
        return buildUuid;
    }

    /**
     * Overrides the application's default bugsnag build UUID
     *
     * @param buildUuid the bugsnag build UUID
     */
    public void setBuildUuid(@Nullable String buildUuid) {
        this.buildUuid = buildUuid;
    }

    /**
     * @return the duration in ms for which the app has been running
     */
    public long getDuration() {
        return durationMs;
    }

    /**
     * Overrides the duration in ms for which the app has been running
     *
     * @param durationMs the new duration in ms
     */
    public void setDuration(long durationMs) {
        this.durationMs = durationMs;
    }

    /**
     * @return the duration in ms for which the app has been running in the foreground
     */
    public long getDurationInForeground() {
        return foregroundMs;
    }

    /**
     * Overrides the duration in ms for which the app has been running in the foreground
     *
     * @param foregroundMs the new duration in ms
     */
    public void setDurationInForeground(long foregroundMs) {
        this.foregroundMs = foregroundMs;
    }

    /**
     * @return whether the app is in the foreground or not
     */
    public boolean isInForeground() {
        return inForeground;
    }

    /**
     * Overrides whether the app is in the foreground or not
     *
     * @param inForeground whether the app is in the foreground
     */
    public void setInForeground(boolean inForeground) {
        this.inForeground = inForeground;
    }

}
