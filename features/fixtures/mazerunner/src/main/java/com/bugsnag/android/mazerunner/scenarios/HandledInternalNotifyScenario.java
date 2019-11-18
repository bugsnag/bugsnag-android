package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.mazerunner.CustomSerializedException;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import androidx.annotation.NonNull;

import java.util.HashMap;

public class HandledInternalNotifyScenario extends Scenario {

    public HandledInternalNotifyScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoTrackSessions(false);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.internalClientNotify(new CustomSerializedException(),
                new HashMap<String, Object>() {{
                    put("severity", "warning");
                    put("severityReason", "handledException");
                }}, false, null);
    }
}

