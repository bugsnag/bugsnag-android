package com.bugsnag.android.testharness;

import com.bugsnag.android.Bugsnag;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class TestHarnessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bugsnag.notify(new RuntimeException("whoops"));
    }

    public void crashApp() {
        throw new RuntimeException("Whoops");
    }

}
