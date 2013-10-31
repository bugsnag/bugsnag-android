package com.bugsnag.android.activity;

import com.bugsnag.android.Bugsnag;
import android.app.Activity;

public class BugsnagActivity extends Activity {
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
