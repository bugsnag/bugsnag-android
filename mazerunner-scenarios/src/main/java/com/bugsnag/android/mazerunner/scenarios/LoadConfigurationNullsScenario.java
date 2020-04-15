package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.BreadcrumbType;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Client;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.EndpointConfiguration;
import com.bugsnag.android.Event;
import com.bugsnag.android.OnErrorCallback;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;


public class LoadConfigurationNullsScenario extends Scenario {

    private Context context;

    public LoadConfigurationNullsScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        this.context = context;
    }

    @Override
    public void run() {
        super.run();
        Configuration testConfig = new Configuration("12312312312312312312312312312312");
        // Setup
        testConfig.setAutoDetectErrors(true);
        testConfig.setAutoTrackSessions(false);
        testConfig.setEndpoints(new EndpointConfiguration("http://bs-local.com:9339", "http://bs-local.com:9339"));

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

        Bugsnag.start(this.context, testConfig);

        Bugsnag.notify(new RuntimeException("LoadConfigurationNullsScenario"));
    }
}
