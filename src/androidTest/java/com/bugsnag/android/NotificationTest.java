package com.bugsnag.android;

import org.json.JSONException;
import org.json.JSONObject;

public class NotificationTest extends BugsnagTestCase {
    public void testInMemoryError() throws JSONException {
        Configuration config = new Configuration("example-api-key");
        Notification notif = new Notification(config);
        notif.addError(new Error(config, new RuntimeException("Something broke")));

        JSONObject notifJson = streamableToJson(notif);
        assertEquals("example-api-key", notifJson.get("apiKey"));
        assertEquals(1, notifJson.getJSONArray("events").length());
    }
}
