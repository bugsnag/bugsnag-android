package com.bugsnag.android;

class TestHooks {
    static boolean getUnhandledOverridden(Event event) {
        return event.impl.getUnhandledOverridden();
    }

    static MetadataState generateMetadataState() {
        return new MetadataState();
    }
}
