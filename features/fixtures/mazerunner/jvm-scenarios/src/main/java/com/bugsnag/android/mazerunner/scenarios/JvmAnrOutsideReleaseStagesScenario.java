package com.bugsnag.android.mazerunner.scenarios;

import static com.bugsnag.android.mazerunner.AnrHelperKt.createDeadlock;
import static com.bugsnag.android.mazerunner.LogKt.getZeroEventsLogMessages;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public class JvmAnrOutsideReleaseStagesScenario extends Scenario {

    /**
     * Initializes bugsnag with custom release stages
     */
    public JvmAnrOutsideReleaseStagesScenario(@NonNull Configuration config,
                                              @NonNull Context context,
                                              @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.setEnabledReleaseStages(Collections.singleton("fee-fi-fo-fum"));
    }

    @Override
    public void startScenario() {
        super.startScenario();
        createDeadlock();
    }

    @Override
    public List<String> getInterceptedLogMessages() {
        return getZeroEventsLogMessages(getStartBugsnagOnly());
    }
}
