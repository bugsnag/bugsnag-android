package com.bugsnag.android;

import android.app.Activity;
import android.os.Bundle;

public class BugsnagActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bugsnag.addActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bugsnag.setContext(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Bugsnag.setContext((String)null);
    }
}