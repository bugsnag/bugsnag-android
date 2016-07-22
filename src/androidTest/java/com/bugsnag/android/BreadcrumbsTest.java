package com.bugsnag.android;

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

public class BreadcrumbsTest extends BugsnagTestCase {
    public void testSerialization() throws JSONException, IOException {
        Breadcrumbs breadcrumbs = new Breadcrumbs();
        breadcrumbs.add("Started app");
        breadcrumbs.add("Clicked a button");
        breadcrumbs.add("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.");

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(3, breadcrumbsJson.length());
        assertEquals("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim", breadcrumbsJson.getJSONObject(2).getJSONObject("metaData").get("message"));
    }

    public void testSizeLimit() throws JSONException, IOException {
        Breadcrumbs breadcrumbs = new Breadcrumbs();
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

    public void testResize() throws JSONException, IOException {
        Breadcrumbs breadcrumbs = new Breadcrumbs();
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

    public void testClear() throws JSONException, IOException {
        Breadcrumbs breadcrumbs = new Breadcrumbs();
        breadcrumbs.add("1");
        breadcrumbs.add("2");
        breadcrumbs.add("3");
        breadcrumbs.clear();

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(0, breadcrumbsJson.length());
    }

    public void testType() throws JSONException, IOException {
        Breadcrumbs breadcrumbs = new Breadcrumbs();
        breadcrumbs.add("1");
        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals("manual", breadcrumbsJson.getJSONObject(0).get("type"));
    }

    public void testPayloadSizeLimit() throws JSONException, IOException {
        Breadcrumbs breadcrumbs = new Breadcrumbs();
        HashMap<String, String> metadata = new HashMap<String, String>();
        for (int i = 0; i < 400; i++) {
            metadata.put(String.format("%d", i), "!!");
        }
        breadcrumbs.add("Rotated Menu", BreadcrumbType.STATE, metadata);
        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(0, breadcrumbsJson.length());
    }

    public void testPayloadType() throws JSONException, IOException {
        Breadcrumbs breadcrumbs = new Breadcrumbs();
        HashMap<String, String> metadata = new HashMap<String, String>();
        metadata.put("direction", "left");
        breadcrumbs.add("Rotated Menu", BreadcrumbType.STATE, metadata);
        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);

        assertEquals("Rotated Menu", breadcrumbsJson.getJSONObject(0).get("name"));
        assertEquals("state", breadcrumbsJson.getJSONObject(0).get("type"));
        assertEquals("left", breadcrumbsJson.getJSONObject(0).getJSONObject("metaData").get("direction"));
        assertEquals(1, breadcrumbsJson.length());
    }
}
