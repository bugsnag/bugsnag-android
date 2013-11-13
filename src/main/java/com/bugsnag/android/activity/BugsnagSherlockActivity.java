package com.bugsnag.android.activity;

import com.bugsnag.android.Bugsnag;
import com.actionbarsherlock.app.SherlockActivity;
import android.os.Bundle;

public class BugsnagSherlockActivity extends SherlockActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bugsnag.onActivityCreate(this);
    }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Bugsnag.onActivityDestroy(this);
    }
}
