package com.bugsnag.android;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJsonArray;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BreadcrumbsTest {

    private Breadcrumbs breadcrumbs;

    @Before
    public void setUp() throws Exception {
        breadcrumbs = new Breadcrumbs();
    }

    @Test
    public void testSerialization() throws JSONException, IOException {
        breadcrumbs.add("Started app");
        breadcrumbs.add("Clicked a button");
        breadcrumbs.add("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.");

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(3, breadcrumbsJson.length());
        assertEquals("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim", breadcrumbsJson.getJSONObject(2).getJSONObject("metaData").get("message"));
    }

    @Test
    public void testSizeLimit() throws JSONException, IOException {
        breadcrumbs.setSize(5);
        breadcrumbs.add("1");
        breadcrumbs.add("2");
        breadcrumbs.add("3");
        breadcrumbs.add("4");
        breadcrumbs.add("5");
        breadcrumbs.add("6");

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(5, breadcrumbsJson.length());
        assertEquals("2", breadcrumbsJson.getJSONObject(0).getJSONObject("metaData").get("message"));
        assertEquals("6", breadcrumbsJson.getJSONObject(4).getJSONObject("metaData").get("message"));
    }

    @Test
    public void testResizePersists() throws JSONException, IOException {
        breadcrumbs.add("1");
        breadcrumbs.add("2");
        breadcrumbs.add("3");
        breadcrumbs.add("4");
        breadcrumbs.add("5");
        breadcrumbs.add("6");
        breadcrumbs.setSize(5);
        breadcrumbs.add("7");

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(5, breadcrumbsJson.length());
        assertEquals("3", breadcrumbsJson.getJSONObject(0).getJSONObject("metaData").get("message"));
        assertEquals("7", breadcrumbsJson.getJSONObject(4).getJSONObject("metaData").get("message"));
    }

    @Test
    public void testResizeEmpty() throws JSONException, IOException {
        breadcrumbs.add("1");
        breadcrumbs.add("2");
        breadcrumbs.setSize(0);
        breadcrumbs.add("3");
        breadcrumbs.add("4");
        breadcrumbs.add("5");
        breadcrumbs.add("6");

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(0, breadcrumbsJson.length());
    }

    @Test
    public void testResizeNegative() throws JSONException, IOException {
        breadcrumbs.add("1");
        breadcrumbs.add("2");
        breadcrumbs.setSize(-1);

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(2, breadcrumbsJson.length());
    }

    @Test
    public void testResize() throws JSONException, IOException {
        breadcrumbs.add("1");
        breadcrumbs.add("2");
        breadcrumbs.add("3");
        breadcrumbs.add("4");
        breadcrumbs.add("5");
        breadcrumbs.add("6");
        breadcrumbs.setSize(5);

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(5, breadcrumbsJson.length());
        assertEquals("2", breadcrumbsJson.getJSONObject(0).getJSONObject("metaData").get("message"));
        assertEquals("6", breadcrumbsJson.getJSONObject(4).getJSONObject("metaData").get("message"));
    }

    @Test
    public void testClear() throws JSONException, IOException {
        breadcrumbs.add("1");
        breadcrumbs.add("2");
        breadcrumbs.add("3");
        breadcrumbs.clear();

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(0, breadcrumbsJson.length());
    }

    @Test
    public void testType() throws JSONException, IOException {
        breadcrumbs.add("1");
        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals("manual", breadcrumbsJson.getJSONObject(0).get("type"));
    }

    @Test
    public void testPayloadSizeLimit() throws JSONException, IOException {
        HashMap<String, String> metadata = new HashMap<>();
        for (int i = 0; i < 400; i++) {
            metadata.put(String.format(Locale.US, "%d", i), "!!");
        }
        breadcrumbs.add("Rotated Menu", BreadcrumbType.STATE, metadata);
        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(0, breadcrumbsJson.length());
    }

    @Test
    public void testPayloadType() throws JSONException, IOException {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("direction", "left");
        breadcrumbs.add("Rotated Menu", BreadcrumbType.STATE, metadata);
        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);

        assertEquals("Rotated Menu", breadcrumbsJson.getJSONObject(0).get("name"));
        assertEquals("state", breadcrumbsJson.getJSONObject(0).get("type"));
        assertEquals("left", breadcrumbsJson.getJSONObject(0).getJSONObject("metaData").get("direction"));
        assertEquals(1, breadcrumbsJson.length());
    }
}
