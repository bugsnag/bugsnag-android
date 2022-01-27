package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.Event;
import com.bugsnag.android.FeatureFlag;
import com.bugsnag.android.OnSendCallback;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

public class CXXFeatureFlagNativeCrashScenario extends Scenario {
    static {
        System.loadLibrary("cxx-scenarios");
    }

    public native void crash();

    /**
     */
    public CXXFeatureFlagNativeCrashScenario(@NonNull Configuration config,
                                             @NonNull Context context,
                                             @Nullable String eventMetadata) {
        super(config, context, eventMetadata);

        if (getEventMetadata() != null && getEventMetadata().contains("onsend")) {
            config.addOnSend(new OnSendCallback() {
                public boolean onSend(@NonNull Event event) {
                    event.addFeatureFlag("on_send_callback");
                    return true;
                }
            });
        }
    }

    @Override
    public void startScenario() {
        super.startScenario();

        Bugsnag.addFeatureFlag("demo_mode");

        Bugsnag.addFeatureFlags(Arrays.asList(
                new FeatureFlag("should_not_be_reported_1"),
                new FeatureFlag("should_not_be_reported_2"),
                new FeatureFlag("should_not_be_reported_3")
        ));

        Bugsnag.clearFeatureFlag("should_not_be_reported_3");
        Bugsnag.clearFeatureFlag("should_not_be_reported_2");
        Bugsnag.clearFeatureFlag("should_not_be_reported_1");

        Bugsnag.addFeatureFlag("demo_mode");
        Bugsnag.addFeatureFlag("sample_group", "a");

        if (getEventMetadata() != null && getEventMetadata().contains("cleared")) {
            Bugsnag.clearFeatureFlags();
        }

        crash();
    }
}
