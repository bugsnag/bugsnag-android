package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.EndpointConfiguration;
import com.bugsnag.android.Event;
import com.bugsnag.android.OnErrorCallback;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LoadConfigurationNullsScenario extends Scenario {

    private final String notifyEndpoint;
    private final String sessionEndpoint;

    /**
     *
     */
    public LoadConfigurationNullsScenario(@NonNull Configuration config,
                                          @NonNull Context context,
                                          @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        this.notifyEndpoint = config.getEndpoints().getNotify();
        this.sessionEndpoint = config.getEndpoints().getSessions();
    }

    @Override
    public void startBugsnag(boolean startBugsnagOnly) {
        setStartBugsnagOnly(startBugsnagOnly);
        Configuration testConfig = new Configuration("12312312312312312312312312312312");
        // Setup
        testConfig.setAutoDetectErrors(true);
        testConfig.setAutoTrackSessions(false);
        testConfig.setEndpoints(new EndpointConfiguration(notifyEndpoint, sessionEndpoint));

        // Nullable options
        testConfig.setAppType(null);
        testConfig.setAppVersion(null);
        testConfig.setContext(null);
        testConfig.setDelivery(null);
        testConfig.setDiscardClasses(null);
        testConfig.setEnabledBreadcrumbTypes(null);
        testConfig.setEnabledReleaseStages(null);
        testConfig.setEndpoints(null);
        testConfig.setLogger(null);
        testConfig.setProjectPackages(null);
        testConfig.setRedactedKeys(null);
        testConfig.setReleaseStage(null);
        testConfig.setSendThreads(null);
        testConfig.setUser(null, null, null);
        testConfig.setVersionCode(null);

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
