package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Timer;
import java.util.TimerTask;

public class CXXAnr_AnrDisabledScenario extends CXXAnrScenario {

    public CXXAnr_AnrDisabledScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.getEnabledErrorTypes().setAnrs(false);
    }

    @Override
    public void run() {
        super.run();

        // Generate a handled event after 2 seconds as a sanity check that the process didn't crash.
		new java.util.Timer(true).schedule( 
		        new java.util.TimerTask() {
		            @Override
		            public void run() {
		                Bugsnag.notify(generateException());
		            }
		        }, 
		        2000
		);

        crash();
    }
}
