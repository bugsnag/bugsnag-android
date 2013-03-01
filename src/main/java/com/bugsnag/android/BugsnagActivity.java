package com.bugsnag.android;

import android.app.Activity;
import android.os.Bundle;

public class BugsnagActivity extends Activity {
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