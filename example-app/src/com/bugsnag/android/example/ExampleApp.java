package com.bugsnag.android.example;

import java.util.Map;
import java.util.HashMap;

import android.app.Activity;
import android.os.Bundle;

import com.bugsnag.android.Bugsnag;

public class ExampleApp extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Register for automatic exception catching
        // In most apps, this is all you will need to do
        Bugsnag.register(this, "08e525b9549090d8dea3ba8c418c5581");

        // Example of setting the release stage, and which release stages we should notify for
        Bugsnag.setReleaseStage("development");
        Bugsnag.setNotifyReleaseStages("production", "development");

        // Example of setting the context to be the name of the activity
        Bugsnag.setContext(this.getClass().getSimpleName());

        // Example of setting global extra data to send with every exception
        Map<String, String> extraData = new HashMap<String,String>();
        extraData.put("users name", "bob hoskins");
        extraData.put("users email", "test@example.com");
        Bugsnag.setExtraData(extraData);

        // Manual notification with metadata example
        Map<String, String> metaData = new HashMap<String,String>();
        metaData.put("example", "metadata");
        metaData.put("more example", "more metadata");
        Bugsnag.notify(new RuntimeException("Bugsnag Android Test Exception"), metaData);

        // Cause a RuntimeException
        throw new RuntimeException("It broke");
    }
}
