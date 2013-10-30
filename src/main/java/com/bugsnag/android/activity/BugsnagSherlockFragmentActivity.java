package com.bugsnag.android.activity;

import com.bugsnag.android.Bugsnag;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import android.os.Bundle;

public class BugsnagSherlockFragmentActivity extends SherlockFragmentActivity {
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
