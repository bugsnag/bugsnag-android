package com.bugsnag.android;

import java.util.Date;
import java.util.List;
import java.util.Map;

class InternalHooks {

    private final Client client;

    public InternalHooks(Client client) {
        this.client = client;
    }

    public ImmutableConfig getConfig() {
        return client.getConfig();
    }

    public AppWithState getAppWithState() {
        return client.appDataCollector.generateAppWithState();
    }

    public Map<String,Object> getAppMetadata() {
        return client.appDataCollector.getAppDataMetadata();
    }

    public Map<String,Object> getDeviceMetadata() {
        return client.deviceDataCollector.getDeviceMetadata();
    }

    public DeviceWithState getDeviceWithState() {
        return client.deviceDataCollector.generateDeviceWithState(new Date().getTime());
    }

    public List<Thread> getThreads(boolean unhandled) {
        return new ThreadState(null, unhandled, getConfig()).getThreads();
    }
}
