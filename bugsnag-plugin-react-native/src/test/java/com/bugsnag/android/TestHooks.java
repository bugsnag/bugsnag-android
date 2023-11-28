package com.bugsnag.android;

class TestHooks {
    static boolean getUnhandledOverridden(Event event) {
        return event.getUnhandledOverridden();
    }

    static MetadataState generateMetadataState() {
        return new MetadataState();
    }

    static FeatureFlagState generateFeatureFlagsState() {
        return new FeatureFlagState();
    }
}
