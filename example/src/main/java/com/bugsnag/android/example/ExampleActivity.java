package com.bugsnag.android.example;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bugsnag.android.Client;
import com.bugsnag.android.Error;
import com.bugsnag.android.MetaData;
import com.bugsnag.android.BeforeNotify;
import com.bugsnag.android.Severity;

public class ExampleActivity extends Activity
{
    private static String BUGSNAG_API_KEY = "066f5ad3590596f9aa8d601ea89af845";
    private Client bugsnag;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Create a bugsnag client
        bugsnag = new Client(this, BUGSNAG_API_KEY);

        // Execute some code before every bugsnag notification
        bugsnag.addBeforeNotify(new BeforeNotify() {
            @Override
            public boolean run(Error error) {
                System.out.println(String.format("In beforeNotify - %s", error.getExceptionName()));
                return true;
            }
        });

        // Set the user information
        bugsnag.setUser("123456", "james@example.com", "James Smith");

        // Add some global metaData
        bugsnag.addToTab("user", "age", 31);
        bugsnag.addToTab("custom", "account", "something");
    }

    public void sendError(View view) {
        bugsnag.notify(new RuntimeException("Non-fatal error"), Severity.ERROR);
        Toast.makeText(this, "Sent error", 1000).show();
    }

    public void sendWarning(View view) {
        bugsnag.notify(new RuntimeException("Non-fatal warning"), Severity.WARNING);
        Toast.makeText(this, "Sent warning", 1000).show();
    }

    public void sendInfo(View view) {
        bugsnag.notify(new RuntimeException("Non-fatal info"), Severity.INFO);
        Toast.makeText(this, "Sent info", 1000).show();
    }

    public void sendErrorWithMetaData(View view) {
        Map<String, String> nested = new HashMap<String, String>();
        nested.put("normalkey", "normalvalue");
        nested.put("password", "s3cr3t");

        MetaData metaData = new MetaData();
        metaData.addToTab("user", "payingCustomer", true);
        metaData.addToTab("user", "password", "p4ssw0rd");
        metaData.addToTab("user", "credentials", nested);

        bugsnag.notify(new RuntimeException("Non-fatal error with metaData"), Severity.ERROR, metaData);
        Toast.makeText(this, "Sent error with metaData", 1000).show();
    }

    public void crash(View view) {
        throw new RuntimeException("Something broke!");
    }
}
