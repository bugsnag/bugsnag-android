package com.bugsnag.android.activity;

import com.bugsnag.android.Bugsnag;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

public class BugsnagFragmentActivity extends FragmentActivity {
    @Override
    protected void onResume() {
        super.onResume();
        Bugsnag.onActivityResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Bugsnag.onActivityPause(this);
    }
}
