package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Breadcrumb;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.Event;
import com.bugsnag.android.OnBreadcrumbCallback;
import com.bugsnag.android.OnErrorCallback;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Map;

/**
 * Sends a handled exception to Bugsnag, which does not include session data.
 */
public class HandledJavaSmokeScenario extends Scenario {

    public HandledJavaSmokeScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoTrackSessions(false);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.addOnBreadcrumb(new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
                Map<String, Object> metadata = breadcrumb.getMetadata();
                metadata.put("source", "BreadcrumbCallback");
                breadcrumb.setMetadata(metadata);
                return true;
            }
        });
        Bugsnag.addMetadata("TestData", "ClientMetadata", true);
        Bugsnag.addOnError(new OnErrorCallback() {
            @Override
            public boolean onError(@NonNull Event event) {
                event.addMetadata("TestData", "CallbackMetadata", true);
                return true;
            }
        });
        Bugsnag.leaveBreadcrumb("HandledJavaSmokeScenario");
        try {
            throw new RuntimeException(getClass().getSimpleName());
        } catch (RuntimeException e) {
            Bugsnag.notify(e);
        }
    }
}
