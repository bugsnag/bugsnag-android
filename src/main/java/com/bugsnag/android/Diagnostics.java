package com.bugsnag.android;

import android.content.Context;

class Diagnostics {
    private Configuration config;
    private Context appContext;

    private AppData appData;
    private DeviceData deviceData;

    public Diagnostics(Configuration config, Context appContext) {
        this.config = config;
        this.appContext = appContext;

        this.appData = new AppData(config, appContext);
        this.deviceData = new DeviceData(config, appContext);
    }

    public AppData getAppData() {
        return appData;
    }

    public DeviceData getDeviceData() {
        return deviceData;
    }

    public AppState getAppState() {
        return new AppState(config, appContext);
    }

    public DeviceState getDeviceState() {
        return new DeviceState(config, appContext);
    }
}
