package com.bugsnag.android.mazerunner.scenarios;

import android.app.Activity;
import android.content.Context;

import com.bugsnag.android.mazerunner.CustomSerializedException;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import androidx.annotation.NonNull;

import java.util.HashMap;

public class CachedHandledInternalNotifyScenario extends Scenario {

    private boolean shouldTestOfflineCaching = false;

    public CachedHandledInternalNotifyScenario(@NonNull Configuration config, 
                                               @NonNull Context context) {
        super(config, context);
        config.setAutoCaptureSessions(false);
        Activity activity = (Activity)context;
        String state = activity.getIntent().getStringExtra("EVENT_METADATA");
        shouldTestOfflineCaching = state != null && state.equals("offline");
        if (shouldTestOfflineCaching) {
            disableAllDelivery(config);
        }
    }

    @Override
    public void run() {
        super.run();
        if (shouldTestOfflineCaching) {
            Bugsnag.internalClientNotify(new CustomSerializedException(),
                    new HashMap<String, Object>() {{
                        put("severity", "warning");
                        put("severityReason", "handledException");
                    }}, false, null);
        }
    }
}

