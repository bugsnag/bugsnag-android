package com.bugsnag.android;

import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.bugsnag.android.example.ExampleActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.StringWriter;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CrashReportTest {

    @Rule
    public ActivityTestRule<ExampleActivity> activityTestRule = new ActivityTestRule<>(ExampleActivity.class);

    private Report report;

    @Test
    @UiThreadTest
    public void checkAppLaunches() throws Exception {
        // send report via activity
        final ExampleActivity activity = activityTestRule.getActivity();
        assertNotNull(activity);

        Bugsnag.setErrorReportApiClient(new ErrorReportApiClient() {
            @Override
            public void postReport(String urlString, Report report) throws NetworkException, BadResponseException {
                // no-op
            }
        });
        activity.sendErrorWithCallback(new Callback() {
            @Override
            public void beforeNotify(Report report) {
                CrashReportTest.this.report = report;
            }
        });

        // validate error report
        assertNotNull(report);
        JSONObject json = getJson(report);

        assertEquals("066f5ad3590596f9aa8d601ea89af845", json.getString("apiKey"));
        assertEquals(3, json.length());

        JSONObject event = json.getJSONArray("events").getJSONObject(0);
        assertNotNull(event);
        assertEquals("ExampleActivity", event.getString("context"));

        JSONArray exceptions = event.getJSONArray("exceptions");
        assertEquals(1, exceptions.length());
        assertNotNull(exceptions.getJSONObject(0));
    }

    private JSONObject getJson(JsonStream.Streamable streamable) throws IOException, JSONException {
        StringWriter writer = new StringWriter();
        JsonStream jsonStream = new JsonStream(writer);
        streamable.toStream(jsonStream);
        return new JSONObject(writer.toString());
    }

}
