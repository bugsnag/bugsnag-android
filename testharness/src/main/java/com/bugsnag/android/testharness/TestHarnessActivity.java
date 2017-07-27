package com.bugsnag.android.testharness;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class TestHarnessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        throw new RuntimeException("Whoops");
    }

}
