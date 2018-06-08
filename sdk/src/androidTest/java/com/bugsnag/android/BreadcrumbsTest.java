package com.bugsnag.android;

import static com.bugsnag.android.BreadcrumbType.MANUAL;
import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJsonArray;
import static org.junit.Assert.assertEquals;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Queue;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class BreadcrumbsTest {

    private Breadcrumbs breadcrumbs;

    @Before
    public void setUp() throws Exception {
        breadcrumbs = new Breadcrumbs();
    }

    @After
    public void tearDown() throws Exception {
        Async.cancelTasks();
    }

    @Test
    public void testSerialization() throws JSONException, IOException {
        breadcrumbs.add(new Breadcrumb("Started app"));
        breadcrumbs.add(new Breadcrumb("Clicked a button"));
        breadcrumbs.add(new Breadcrumb("Lorem ipsum dolor sit amet, consectetur adipiscing elit,"
            + " sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad "
            + "minim veniam, quis nostrud exercitation ullamco laboris nisi"
            + " ut aliquip ex ea commodo consequat."));

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(3, breadcrumbsJson.length());
        assertEquals("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do "
                + "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim",
            breadcrumbsJson.getJSONObject(2).getJSONObject("metaData").get("message"));
    }

    @Test
    public void testSizeLimit() throws JSONException, IOException {
        breadcrumbs.setSize(5);
        breadcrumbs.add(new Breadcrumb("1"));
        breadcrumbs.add(new Breadcrumb("2"));
        breadcrumbs.add(new Breadcrumb("3"));
        breadcrumbs.add(new Breadcrumb("4"));
        breadcrumbs.add(new Breadcrumb("5"));
        breadcrumbs.add(new Breadcrumb("6"));

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(5, breadcrumbsJson.length());

        JSONObject firstJsonNode = breadcrumbsJson.getJSONObject(0);
        JSONObject otherJsonNode = breadcrumbsJson.getJSONObject(4);

        assertEquals("2", firstJsonNode.getJSONObject("metaData").get("message"));
        assertEquals("6", otherJsonNode.getJSONObject("metaData").get("message"));
    }

    @Test
    public void testResizePersists() throws JSONException, IOException {
        breadcrumbs.add(new Breadcrumb("1"));
        breadcrumbs.add(new Breadcrumb("2"));
        breadcrumbs.add(new Breadcrumb("3"));
        breadcrumbs.add(new Breadcrumb("4"));
        breadcrumbs.add(new Breadcrumb("5"));
        breadcrumbs.add(new Breadcrumb("6"));
        breadcrumbs.setSize(5);
        breadcrumbs.add(new Breadcrumb("7"));

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(5, breadcrumbsJson.length());

        JSONObject firstJsonNode = breadcrumbsJson.getJSONObject(0);
        JSONObject otherJsonNode = breadcrumbsJson.getJSONObject(4);

        assertEquals("3", firstJsonNode.getJSONObject("metaData").get("message"));
        assertEquals("7", otherJsonNode.getJSONObject("metaData").get("message"));
    }

    @Test
    public void testResizeEmpty() throws JSONException, IOException {
        breadcrumbs.add(new Breadcrumb("1"));
        breadcrumbs.add(new Breadcrumb("2"));
        breadcrumbs.setSize(0);
        breadcrumbs.add(new Breadcrumb("3"));
        breadcrumbs.add(new Breadcrumb("4"));
        breadcrumbs.add(new Breadcrumb("5"));
        breadcrumbs.add(new Breadcrumb("6"));

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(0, breadcrumbsJson.length());
    }

    @Test
    public void testResizeNegative() throws JSONException, IOException {
        breadcrumbs.add(new Breadcrumb("1"));
        breadcrumbs.add(new Breadcrumb("2"));
        breadcrumbs.setSize(-1);

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(2, breadcrumbsJson.length());
    }

    @Test
    public void testResize() throws JSONException, IOException {
        breadcrumbs.add(new Breadcrumb("1"));
        breadcrumbs.add(new Breadcrumb("2"));
        breadcrumbs.add(new Breadcrumb("3"));
        breadcrumbs.add(new Breadcrumb("4"));
        breadcrumbs.add(new Breadcrumb("5"));
        breadcrumbs.add(new Breadcrumb("6"));
        breadcrumbs.setSize(5);

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(5, breadcrumbsJson.length());

        JSONObject firstJsonNode = breadcrumbsJson.getJSONObject(0);
        JSONObject otherJsonNode = breadcrumbsJson.getJSONObject(4);

        assertEquals("2", firstJsonNode.getJSONObject("metaData").get("message"));
        assertEquals("6", otherJsonNode.getJSONObject("metaData").get("message"));
    }

    @Test
    public void testClear() throws JSONException, IOException {
        breadcrumbs.add(new Breadcrumb("1"));
        breadcrumbs.add(new Breadcrumb("2"));
        breadcrumbs.add(new Breadcrumb("3"));
        breadcrumbs.clear();

        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(0, breadcrumbsJson.length());
    }

    @Test
    public void testType() throws JSONException, IOException {
        breadcrumbs.add(new Breadcrumb("1"));
        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals("manual", breadcrumbsJson.getJSONObject(0).get("type"));
    }

    @Test
    public void testPayloadSizeLimit() throws JSONException, IOException {
        HashMap<String, String> metadata = new HashMap<>();
        for (int i = 0; i < 400; i++) {
            metadata.put(String.format(Locale.US, "%d", i), "!!");
        }
        breadcrumbs.add(new Breadcrumb("Rotated Menu", BreadcrumbType.STATE, metadata));
        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);
        assertEquals(0, breadcrumbsJson.length());
    }

    @Test
    public void testPayloadType() throws JSONException, IOException {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("direction", "left");
        breadcrumbs.add(new Breadcrumb("Rotated Menu", BreadcrumbType.STATE, metadata));
        JSONArray breadcrumbsJson = streamableToJsonArray(breadcrumbs);

        JSONObject node = breadcrumbsJson.getJSONObject(0);
        assertEquals("Rotated Menu", node.get("name"));
        assertEquals("state", node.get("type"));
        assertEquals("left", node.getJSONObject("metaData").get("direction"));
        assertEquals(1, breadcrumbsJson.length());
    }

    @Test
    public void testClientMethods() {
        Client client = generateClient();
        client.leaveBreadcrumb("Hello World");
        Queue<Breadcrumb> store = client.breadcrumbs.store;
        int count = 0;

        for (Breadcrumb breadcrumb : store) {
            if (MANUAL == breadcrumb.getType() && "manual".equals(breadcrumb.getName())) {
                count++;
                assertEquals("Hello World", breadcrumb.getMetadata().get("message"));
            }
        }
        assertEquals(1, count);
    }

}
