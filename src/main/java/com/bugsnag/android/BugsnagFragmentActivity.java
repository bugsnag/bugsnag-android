package com.bugsnag.android;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

public class BugsnagFragmentActivity extends FragmentActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStack.add(this);
        ActivityStack.setTopActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityStack.setTopActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ActivityStack.clearTopActivity();
    }
}
