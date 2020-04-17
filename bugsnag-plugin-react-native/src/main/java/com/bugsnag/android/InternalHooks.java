package com.bugsnag.android;

import java.util.Date;
import java.util.List;

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

    public DeviceWithState getDeviceWithState() {
        return client.deviceDataCollector.generateDeviceWithState(new Date().getTime());
    }

    public List<Breadcrumb> getBreadcrumbs() {
        return client.getBreadcrumbs();
    }

    public List<Thread> getThreads(boolean unhandled) {
        return new ThreadState(null, unhandled, getConfig()).getThreads();
    }
}
