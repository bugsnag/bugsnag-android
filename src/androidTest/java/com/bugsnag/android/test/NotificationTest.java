package com.bugsnag.android;

import org.json.JSONObject;

import com.bugsnag.android.Configuration;
import com.bugsnag.android.Notification;

public class NotificationTest extends BugsnagTestCase {
    public void testInMemoryError() throws org.json.JSONException {
        Configuration config = new Configuration("example-api-key");
        Notification notif = new Notification(config);
        notif.addError(new Error(config, new RuntimeException("Something broke")));

        JSONObject notifJson = streamableToJson(notif);
        assertEquals(notifJson.get("apiKey"), "example-api-key");
        assertEquals(notifJson.getJSONArray("events").length(), 1);
    }
}
