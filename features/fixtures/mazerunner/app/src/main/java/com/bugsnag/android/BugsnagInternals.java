package com.bugsnag.android;

public class BugsnagInternals {
    private BugsnagInternals() {}

    public static void flush() {
        Bugsnag.client.getEventStore().flushAsync();
    }
}
