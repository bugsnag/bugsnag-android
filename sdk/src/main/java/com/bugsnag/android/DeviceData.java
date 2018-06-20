package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * Information about the current Android device which doesn't change over time,
 * including screen and locale information.
 */
public class DeviceData extends DeviceDataSummary {

    private long freeMemory;
    private long totalMemory;

    @Nullable
    private Long freeDisk;

    @Nullable
    private String id;

    @Nullable
    private String orientation;

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        serialiseMinimalDeviceData(writer);

        writer
            .name("id").value(id)
            .name("freeMemory").value(freeMemory)
            .name("totalMemory").value(totalMemory)
            .name("freeDisk").value(freeDisk)
            .name("orientation").value(orientation);
        writer.endObject();
    }

    /**
     * @return the device's unique ID for the current app installation
     */
    @Nullable
    public String getId() {
        return id;
    }

    /**
     * Overrides the device's unique ID. This can be set to null for privacy reasons, if desired.
     *
     * @param id the new device id
     */
    public void setId(@Nullable String id) {
        this.id = id;
    }

    /**
     * @return the amount of free memory in bytes that the VM can allocate
     */
    public long getFreeMemory() {
        return freeMemory;
    }

    /**
     * Overrides the default value for the device's free memory.
     *
     * @param freeMemory the new free memory value, in bytes
     */
    public void setFreeMemory(long freeMemory) {
        this.freeMemory = freeMemory;
    }

    /**
     * @return the total amount of memory in bytes that the VM can allocate
     */
    public long getTotalMemory() {
        return totalMemory;
    }

    /**
     * Overrides the default value for the device's total memory.
     *
     * @param totalMemory the new total memory value, in bytes
     */
    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    /**
     * @return the amount of disk space available on the smallest disk on the device, if known
     */
    @Nullable
    public Long getFreeDisk() {
        return freeDisk;
    }

    /**
     * Overrides the default value for the device's free disk space, in bytes.
     *
     * @param freeDisk the new free disk space, in bytes
     */
    public void setFreeDisk(@Nullable Long freeDisk) {
        this.freeDisk = freeDisk;
    }

    /**
     * @return the device's orientation, if known
     */
    @Nullable
    public String getOrientation() {
        return orientation;
    }

    /**
     * Overrides the device's default orientation
     *
     * @param orientation the new orientation
     */
    public void setOrientation(@Nullable String orientation) {
        this.orientation = orientation;
    }

}
