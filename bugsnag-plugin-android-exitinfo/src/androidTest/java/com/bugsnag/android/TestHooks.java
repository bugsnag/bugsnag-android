package com.bugsnag.android;

import com.bugsnag.android.internal.ImmutableConfig;
import com.bugsnag.android.internal.ImmutableConfigKt;

import androidx.annotation.NonNull;

public class TestHooks {
    private TestHooks() {
    }

    public static ImmutableConfig convertToImmutableConfig(
            @NonNull Configuration configuration
    ) {
        return ImmutableConfigKt.convertToImmutableConfig(configuration);
    }
}
