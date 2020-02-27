package com.bugsnag.android;

import org.jetbrains.annotations.NotNull;

public class BugsnagReactNativePlugin implements Plugin {

    @Override
    public void load(@NotNull Client client) {
        client.immutableConfig.getLogger().i("Initialized React Native Plugin");
    }

    @Override
    public void unload() {

    }
}
