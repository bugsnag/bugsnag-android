package com.bugsnag.android.example;

import android.app.Activity;
import android.os.Bundle;

import com.bugsnag.android.Bugsnag;

public class ExampleApp extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Bugsnag.register(this, "08e525b9549090d8dea3ba8c418c5581");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Cause a NullPointerException
        Activity nullActivity = null;
        nullActivity.getApplication();
        
        // Cause a RuntimeException
        throw new RuntimeException("It broke");
    }
}
