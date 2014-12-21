package com.bugsnag.android;

import org.json.JSONException;
import org.json.JSONArray;

public class BreadcrumbsTest extends BugsnagTestCase {
    public void testSerialization() throws JSONException {
        Breadcrumbs breadcrumbs = new Breadcrumbs();
        breadcrumbs.add("Started app");
        breadcrumbs.add("Clicked a button");
        breadcrumbs.add("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.");

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(3, breadcrumbsJson.length());
        assertEquals("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim", breadcrumbsJson.getJSONArray(2).get(1));
    }

    public void testSizeLimit() throws JSONException {
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
        assertEquals("2", breadcrumbsJson.getJSONArray(0).get(1));
        assertEquals("6", breadcrumbsJson.getJSONArray(4).get(1));
    }

    public void testResize() throws JSONException {
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
        assertEquals("2", breadcrumbsJson.getJSONArray(0).get(1));
        assertEquals("6", breadcrumbsJson.getJSONArray(4).get(1));
    }

    public void testClear() throws JSONException {
        Breadcrumbs breadcrumbs = new Breadcrumbs();
        breadcrumbs.add("1");
        breadcrumbs.add("2");
        breadcrumbs.add("3");
        breadcrumbs.clear();

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(0, breadcrumbsJson.length());
    }
}
