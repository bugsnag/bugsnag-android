package com.bugsnag.android;

class TestHooks {
    static boolean getUnhandledOverridden(Event event) {
        return event.getImpl().getUnhandledOverridden();
    }
}
