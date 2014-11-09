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
    }

    AppData getAppData() {
        if(appData == null) {
            appData = new AppData(config, appContext);
        }
        return appData;
    }

    DeviceData getDeviceData() {
        if(deviceData == null) {
            deviceData = new DeviceData(appContext);
        }
        return deviceData;
    }

    AppState getAppState() {
        return new AppState(appContext);
    }

    DeviceState getDeviceState() {
        return new DeviceState(appContext);
    }
}
