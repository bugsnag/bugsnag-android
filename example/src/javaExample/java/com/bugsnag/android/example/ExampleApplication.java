package com.bugsnag.android.example;

import android.app.Application;

import com.bugsnag.android.BadResponseException;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.NetworkException;
import com.bugsnag.android.SessionTrackingApiClient;
import com.bugsnag.android.SessionTrackingPayload;

import java.util.Map;

public class ExampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the Bugsnag client
//        Bugsnag.init(this, "api-key");


        Configuration config = new Configuration("your-api-key");
        config.setAutoCaptureSessions(true);
        config.setSessionEndpoint("http://sessions.example.com");
        Bugsnag.init(this, config);

        Bugsnag.startSession();
        Bugsnag.setSessionTrackingApiClient(new SessionTrackingApiClient() {
            @Override
            public void postSessionTrackingPayload(String urlString,
                                                   SessionTrackingPayload payload,
                                                   Map<String, String> headers)
                throws NetworkException, BadResponseException {

            }
        });
    }

}
