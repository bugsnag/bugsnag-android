package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;

public class CXXAnr_NdkDisabledScenario extends CXXAnrScenario {

    public CXXAnr_NdkDisabledScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.getEnabledErrorTypes().setAnrs(true);
        config.getEnabledErrorTypes().setNdkCrashes(false);
    }
}
