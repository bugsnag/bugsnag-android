package com.bugsnag.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.Map;

final class BugsnagTestUtils {

    private BugsnagTestUtils() {
    }

    private static String streamableToString(JsonStream.Streamable streamable) throws IOException {
        StringWriter writer = new StringWriter();
        JsonStream jsonStream = new JsonStream(writer);
        streamable.toStream(jsonStream);
        return writer.toString();
    }

    static JSONObject mapToJson(Map<String, Object> map) {
        return new JSONObject(map);
    }

    static JSONObject streamableToJson(JsonStream.Streamable streamable)
        throws JSONException, IOException {
        return new JSONObject(streamableToString(streamable));
    }

    static JSONArray streamableToJsonArray(JsonStream.Streamable streamable)
        throws JSONException, IOException {
        return new JSONArray(streamableToString(streamable));
    }

    static SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences("com.bugsnag.android", Context.MODE_PRIVATE);
    }

    static Client generateClient() {
        Client client = new Client(InstrumentationRegistry.getContext(), "api-key");
        client.config.setDelivery(generateDelivery());
        return client;
    }

    static Session generateSession() {
        return new Session("test", new Date(), new User(), false);
    }

    static Configuration generateConfiguration() {
        Configuration configuration = new Configuration("test");
        configuration.setDelivery(generateDelivery());
        return configuration;
    }

    static SessionTracker generateSessionTracker() {
        return new SessionTracker(generateConfiguration(), BugsnagTestUtils.generateClient(),
            generateSessionStore());
    }

    @NonNull
    static SessionStore generateSessionStore() {
        return new SessionStore(generateConfiguration(), InstrumentationRegistry.getContext());
    }

    @SuppressWarnings("deprecation")
    @NonNull
    static SessionTrackingApiClient generateSessionTrackingApiClient() {
        return new SessionTrackingApiClient() {
            @Override
            public void postSessionTrackingPayload(String urlString,
                                                   SessionTrackingPayload payload,
                                                   Map<String, String> headers)
                throws NetworkException, BadResponseException {

            }
        };
    }

    @SuppressWarnings("deprecation")
    static ErrorReportApiClient generateErrorReportApiClient() { // no-op
        return new ErrorReportApiClient() {
            @Override
            public void postReport(String urlString,
                                   Report report,
                                   Map<String, String> headers)
                throws NetworkException, BadResponseException {

            }
        };
    }

    public static Delivery generateDelivery() {
        return new Delivery() {
            @Override
            public void deliver(SessionTrackingPayload payload,
                                Configuration config)
                throws DeliveryFailureException {}

            @Override
            public void deliver(Report report,
                                Configuration config)
                throws DeliveryFailureException {}

        };
    }
}
