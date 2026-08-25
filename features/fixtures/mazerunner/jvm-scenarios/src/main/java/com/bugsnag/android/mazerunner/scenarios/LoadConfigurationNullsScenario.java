package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.Event;
import com.bugsnag.android.OnErrorCallback;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LoadConfigurationNullsScenario extends Scenario {

    /**
     *
     */
    public LoadConfigurationNullsScenario(@NonNull Configuration config,
                                          @NonNull Context context,
                                          @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
    }

    @Override
    public void startBugsnag(boolean startBugsnagOnly) {
        setStartBugsnagOnly(startBugsnagOnly);
        Configuration testConfig = new Configuration("12312312312312312312312312312312");

        // Setup
        testConfig.setAutoDetectErrors(true);
        testConfig.setAutoTrackSessions(false);

        // Nullable options (test that null values are handled gracefully)
        testConfig.setAppType(null);
        testConfig.setAppVersion(null);
        testConfig.setContext(null);
        testConfig.setDelivery(null);
        testConfig.setDiscardClasses(null);
        testConfig.setEnabledBreadcrumbTypes(null);
        testConfig.setEnabledReleaseStages(null);
        testConfig.setEndpoints(null);       // ← test null first
        testConfig.setLogger(null);
        testConfig.setProjectPackages(null);
        testConfig.setRedactedKeys(null);
        testConfig.setReleaseStage(null);
        testConfig.setSendThreads(null);
        testConfig.setUser(null, null, null);
        testConfig.setVersionCode(null);

        // Restore the full Maze Runner endpoint configuration after exercising null values.
        testConfig.setEndpoints(getConfig().getEndpoints());

        testConfig.addOnError(new OnErrorCallback() {
            @Override
            public boolean onError(Event event) {
                event.addMetadata("test", "foo", "bar");
                event.addMetadata("test", "filter_me", "foobar");
                return true;
            }
        });

        Bugsnag.start(getContext(), testConfig);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        Bugsnag.notify(new RuntimeException("LoadConfigurationNullsScenario"));
    }
}
