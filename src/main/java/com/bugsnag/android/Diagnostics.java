package com.bugsnag.android;

import android.content.Context;

/**
 * Device and application diagnostic information.
 */
class Diagnostics {
    private Configuration config;

    private AppData appData;
    private AppState appState;
    private DeviceData deviceData;
    private DeviceState deviceState;

    Diagnostics(Configuration config, Context appContext) {
        this.config = config;

        appData = new AppData(appContext, config);
        appState = new AppState(appContext);
        deviceData = new DeviceData(appContext);
        deviceState = new DeviceState(appContext);
    }

    AppData getAppData() {
        return appData;
    }

    DeviceData getDeviceData() {
        return deviceData;
    }

    AppState getAppState() {
        return appState;
    }

    DeviceState getDeviceState() {
        return deviceState;
    }

    String getReleaseStage() {
        if(config.releaseStage != null) {
            return config.releaseStage;
        } else {
            return appData.getReleaseStage();
        }
    }

    String getDeviceId() {
        return deviceData.getAndroidId();
    }

    String getContext() {
        if(config.context != null) {
            return config.context;
        } else {
            String activeScreen = appState.getActiveScreen();
            if(activeScreen != null) {
                return activeScreen.substring(activeScreen.lastIndexOf('.') + 1);
            } else {
                return null;
            }
        }
    }
}
