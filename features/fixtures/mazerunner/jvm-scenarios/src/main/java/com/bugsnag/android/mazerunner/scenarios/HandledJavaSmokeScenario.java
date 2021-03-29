package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Breadcrumb;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.Event;
import com.bugsnag.android.OnBreadcrumbCallback;
import com.bugsnag.android.OnErrorCallback;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

/**
 * Sends a handled exception to Bugsnag, which does not include session data.
 */
public class HandledJavaSmokeScenario extends Scenario {

    public HandledJavaSmokeScenario(@NonNull Configuration config,
                                    @NonNull Context context,
                                    @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        Bugsnag.setContext("FooContext");
        Bugsnag.addOnBreadcrumb(new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
                Map<String, Object> metadata = breadcrumb.getMetadata();
                metadata.put("source", "BreadcrumbCallback");
                breadcrumb.setMetadata(metadata);
                return true;
            }
        });
        Bugsnag.addMetadata("TestData", "password", "NotTellingYou");
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
            throw new IllegalStateException(getClass().getSimpleName());
        } catch (RuntimeException exc) {
            Bugsnag.notify(exc);
        }
    }
}
