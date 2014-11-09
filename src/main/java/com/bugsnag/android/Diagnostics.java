package com.bugsnag.android;

import android.content.Context;

class Diagnostics {
    private Configuration config;
    private Context appContext;

    private AppData appData;
    private DeviceData deviceData;

    Diagnostics(Configuration config, Context appContext) {
        this.config = config;
        this.appContext = appContext;

        this.appData = new AppData(config, appContext);
        this.deviceData = new DeviceData(config, appContext);
    }

    AppData getAppData() {
        return appData;
    }

    DeviceData getDeviceData() {
        return deviceData;
    }

    AppState getAppState() {
        return new AppState(config, appContext);
    }

    DeviceState getDeviceState() {
        return new DeviceState(config, appContext);
    }
}
