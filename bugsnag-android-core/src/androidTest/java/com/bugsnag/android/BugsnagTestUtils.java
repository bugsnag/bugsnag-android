package com.bugsnag.android;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

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

    static Client generateClient(Configuration config) {
        config.setDelivery(generateDelivery());
        return new Client(ApplicationProvider.getApplicationContext(), config);
    }

    static Client generateClient() {
        return generateClient(new Configuration("api-key"));
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

    static Connectivity generateConnectivity() {
        return new ConnectivityCompat(ApplicationProvider.getApplicationContext(), null);
    }

    @NonNull
    static SessionStore generateSessionStore() {
        Context applicationContext = ApplicationProvider.getApplicationContext();
        return new SessionStore(generateConfiguration(), null);
    }

    @SuppressWarnings("deprecation")
    @NonNull
    static SessionTrackingApiClient generateSessionTrackingApiClient() {
        return new SessionTrackingApiClient() {
            @Override
            public void postSessionTrackingPayload(@NonNull String urlString,
                                                   @NonNull SessionTrackingPayload payload,
                                                   @NonNull Map<String, String> headers)
                throws NetworkException, BadResponseException {

            }
        };
    }

    @SuppressWarnings("deprecation")
    static ErrorReportApiClient generateErrorReportApiClient() { // no-op
        return new ErrorReportApiClient() {
            @Override
            public void postReport(@NonNull String urlString,
                                   @NonNull Report report,
                                   @NonNull Map<String, String> headers)
                throws NetworkException, BadResponseException {

            }
        };
    }

    public static Delivery generateDelivery() {
        return new Delivery() {
            @Override
            public void deliver(@NonNull SessionTrackingPayload payload,
                                @NonNull Configuration config)
                throws DeliveryFailureException {}

            @Override
            public void deliver(@NonNull Report report,
                                @NonNull Configuration config)
                throws DeliveryFailureException {}

        };
    }
}
