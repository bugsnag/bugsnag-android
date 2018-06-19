package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.IOException;

class DeviceDataSummary implements JsonStream.Streamable {

    private boolean rooted = isRooted();
    private String manufacturer;
    private String model;
    private String osName;
    private String osVersion;

    DeviceDataSummary() {
        manufacturer = android.os.Build.MANUFACTURER;
        model = android.os.Build.MODEL;
        osName = "android";
        osVersion = android.os.Build.VERSION.RELEASE;
    }

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

    public boolean isJailBroken() {
        return rooted;
    }

    public void setJailbroken(boolean jailbroken) {
        this.rooted = jailbroken;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    private static final String[] ROOT_INDICATORS = new String[]{
        // Common binaries
        "/system/xbin/su",
        "/system/bin/su",
        // < Android 5.0
        "/system/app/Superuser.apk",
        "/system/app/SuperSU.apk",
        // >= Android 5.0
        "/system/app/Superuser",
        "/system/app/SuperSU",
        // Fallback
        "/system/xbin/daemonsu",
        // Systemless root
        "/su/bin"
    };

    /**
     * Check if the current Android device is rooted
     */
    static boolean isRooted() {
        if (android.os.Build.TAGS != null && android.os.Build.TAGS.contains("test-keys")) {
            return true;
        }

        try {
            for (String candidate : ROOT_INDICATORS) {
                if (new File(candidate).exists()) {
                    return true;
                }
            }
        } catch (Exception ignore) {
        }
        return false;
    }

}
