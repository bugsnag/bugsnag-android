package com.bugsnag.android.example;

import java.util.Map;
import java.util.HashMap;

import android.app.Activity;
import android.os.Bundle;

import com.bugsnag.android.*;

public class ExampleApp extends BugsnagActivity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Register for automatic exception catching
        // In most apps, this is all you will need to do
        Bugsnag.register(this, "75d592d5c4652be5bb1ef0788d34eb33");

        // Example of setting the release stage, and which release stages we should notify for
        Bugsnag.setReleaseStage("development");
        Bugsnag.setNotifyReleaseStages("production", "development");

        // Example of setting the context to be the name of the activity
        // If you make your activity inherit from BugsnagActivity, you will get this for free
        Bugsnag.setContext(this.getClass().getSimpleName());

        // Example of setting global extra data to send with every exception
        Bugsnag.addToTab("user", "name", "bob hoskins");
        Bugsnag.addToTab("user", "email", "test@example.com");
        
        Map<String, Object> tabDict = new HashMap<String,Object>();
        Map<String, String> userData = new HashMap<String,String>();
        userData.put("userKey", "userValue");
        tabDict.put("user", userData);
        Bugsnag.notify(new RuntimeException("Bugsnag Android Test Exception"), tabDict);
        
        Map<String, String> deviceData = new HashMap<String,String>();
        deviceData.put("deviceKey", "deviceValue");
        tabDict.put("device", deviceData);
        Bugsnag.notify(new RuntimeException("Bugsnag Android Test Exception"), tabDict);

        tabDict.put("customKey", "customValue");
        Bugsnag.notify(new RuntimeException("Bugsnag Android Test Exception"), tabDict);

        // Cause a RuntimeException
        //throw new RuntimeException("It broke");
    }
}
