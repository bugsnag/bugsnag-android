package com.bugsnag.android;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class InternalHooks {

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

    // TODO: need to return true values for threads, which requires extracting the
    //  ThreadPolicy logic which determines whether to collect them or not from the Event
    //  into the ThreadState constructor within bugsnag-android. Using a dummy value for now
    public List<Thread> getThreads() {
        return Collections.emptyList();
    }
}
