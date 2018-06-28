package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;

public class DeviceDataSummary implements JsonStream.Streamable {

    private boolean rooted;

    @SuppressWarnings("NullableProblems") // set after initialisation
    @NonNull
    private String manufacturer;

    @SuppressWarnings("NullableProblems") // set after initialisation
    @NonNull
    private String model;

    @SuppressWarnings("NullableProblems") // set after initialisation
    @NonNull
    private String osName;

    @SuppressWarnings("NullableProblems") // set after initialisation
    @NonNull
    private String osVersion;

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        serialiseMinimalDeviceData(writer);
        writer.endObject();
    }

    void serialiseMinimalDeviceData(@NonNull JsonStream writer) throws IOException {
        writer
            .name("manufacturer").value(manufacturer)
            .name("model").value(model)
            .name("jailbroken").value(rooted)
            .name("osName").value(osName)
            .name("osVersion").value(osVersion);
    }

    /**
     * @return true if the device is rooted, otherwise false
     */
    public boolean isJailbroken() {
        return rooted;
    }

    /**
     * Overrides whether the device is rooted or not
     *
     * @param jailbroken true if the device is rooted
     */
    public void setJailbroken(boolean jailbroken) {
        this.rooted = jailbroken;
    }

    /**
     * @return the device manufacturer, determined via {@link android.os.Build#MANUFACTURER}
     */
    @NonNull
    public String getManufacturer() {
        return manufacturer;
    }

    /**
     * Overrides the device manufacturer
     *
     * @param manufacturer the new manufacturer
     */
    public void setManufacturer(@NonNull String manufacturer) {
        this.manufacturer = manufacturer;
    }

    /**
     * @return the device model, determined via {@link android.os.Build#MODEL}
     */
    @NonNull
    public String getModel() {
        return model;
    }

    /**
     * Overrides the device model
     *
     * @param model the new device model
     */
    public void setModel(@NonNull String model) {
        this.model = model;
    }

    /**
     * @return the osName, 'Android' by default
     */
    @NonNull
    public String getOsName() {
        return osName;
    }

    /**
     * Overrides the default osName
     *
     * @param osName the new osName
     */
    public void setOsName(@NonNull String osName) {
        this.osName = osName;
    }

    /**
     * @return the device operating system, determined via {@link android.os.Build.VERSION#RELEASE}
     */
    @NonNull
    public String getOsVersion() {
        return osVersion;
    }

    /**
     * Overrides the device operating system version
     *
     * @param osVersion the new os version
     */
    public void setOsVersion(@NonNull String osVersion) {
        this.osVersion = osVersion;
    }

}
