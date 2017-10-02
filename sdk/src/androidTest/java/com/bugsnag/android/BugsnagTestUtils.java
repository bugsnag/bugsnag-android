package com.bugsnag.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;

final class BugsnagTestUtils {

    private BugsnagTestUtils() {
    }

    private static String streamableToString(JsonStream.Streamable streamable) throws IOException {
        StringWriter writer = new StringWriter();
        JsonStream jsonStream = new JsonStream(writer);
        streamable.toStream(jsonStream);
        return writer.toString();
    }

    static JSONObject streamableToJson(JsonStream.Streamable streamable) throws JSONException, IOException {
        return new JSONObject(streamableToString(streamable));
    }

    static JSONArray streamableToJsonArray(JsonStream.Streamable streamable) throws JSONException, IOException {
        return new JSONArray(streamableToString(streamable));
    }

    static SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences("com.bugsnag.android", Context.MODE_PRIVATE);
    }

    static Client generateClient() {
        Client client = new Client(InstrumentationRegistry.getContext(), "api-key");
        client.setErrorReportApiClient(generateErrorReportApiClient());
        return client;
    }

    static ErrorReportApiClient generateErrorReportApiClient() { // no-op
        return new ErrorReportApiClient() {
            @Override
            public void postReport(String urlString, Report report) throws NetworkException, BadResponseException {

            }
        };
    }
}
