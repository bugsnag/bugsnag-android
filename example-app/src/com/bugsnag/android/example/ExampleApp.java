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
        Bugsnag.register(this, "cbefb176c33de28d638d2b8002f26146");
        Bugsnag.setEndpoint("http://192.168.1.115:8000/notify");
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Cause a NullPointerException
        // Activity nullActivity = null;
        // nullActivity.getApplication();
        
        // Cause a RuntimeException
        throw new RuntimeException("It broke");
    }
}
