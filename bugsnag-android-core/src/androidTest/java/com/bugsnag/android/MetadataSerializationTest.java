package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.SmallTest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SmallTest
public class MetadataSerializationTest {

    private Client client;

    @Before
    public void setUp() throws Exception {
        client = BugsnagTestUtils.generateClient();
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void testBasicSerialization() throws JSONException, IOException {
        Metadata metadata = new Metadata();
        metadata.addMetadata("example", "string", "value");
        metadata.addMetadata("example", "integer", 123);
        metadata.addMetadata("example", "double", 123.45);
        metadata.addMetadata("example", "boolean", true);
        metadata.addMetadata("example", "null", null);
        metadata.addMetadata("example", "array", new String[]{"a", "b"});
        List<String> strings = Arrays.asList("Hello", "World");
        metadata.addMetadata("example", "collection", strings);

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        metadata.addMetadata("example", "map", map);

        JSONObject metadataJson = streamableToJson(metadata);
        assertTrue(metadataJson.has("example"));

        JSONObject tab = metadataJson.getJSONObject("example");
        assertEquals("value", tab.getString("string"));
        assertEquals(123, tab.getInt("integer"));
        assertEquals(123.45, tab.getDouble("double"), 0.01);
        assertEquals(true, tab.getBoolean("boolean"));
        assertTrue(tab.isNull("null"));

        JSONArray array = tab.getJSONArray("array");
        assertEquals(2, array.length());
        assertEquals("a", array.get(0));
        assertEquals("b", array.get(1));

        JSONArray collection = tab.getJSONArray("collection");
        assertEquals(2, collection.length());
        assertEquals("Hello", collection.get(0));
        assertEquals("World", collection.get(1));

        JSONObject mapJson = tab.getJSONObject("map");
        assertEquals("value", mapJson.getString("key"));
    }

    @Test
    public void testNestedMapSerialization() throws JSONException, IOException {
        Map<String, String> childMap = new HashMap<>();
        childMap.put("key", "value");

        Map<String, Object> map = new HashMap<>();
        map.put("key", childMap);

        Metadata metadata = new Metadata();
        metadata.addMetadata("example", "map", map);

        JSONObject metadataJson = streamableToJson(metadata);
        assertTrue(metadataJson.has("example"));

        JSONObject example = metadataJson.getJSONObject("example");
        JSONObject childMapJson = example.getJSONObject("map").getJSONObject("key");
        assertEquals("value", childMapJson.getString("key"));
    }

    @Test
    public void testNestedCollectionSerialization() throws JSONException, IOException {
        Collection<String> childList = new LinkedList<>();
        childList.add("james");
        childList.add("test");

        Collection<Collection<String>> list = new LinkedList<>();
        list.add(childList);

        Metadata metadata = new Metadata();
        metadata.addMetadata("example", "list", list);

        JSONObject metadataJson = streamableToJson(metadata);
        assertTrue(metadataJson.has("example"));

        JSONArray jsonArray = metadataJson.getJSONObject("example").getJSONArray("list");
        JSONArray childListJson = jsonArray.getJSONArray(0);
        assertEquals(2, childListJson.length());
        assertEquals("james", childListJson.get(0));
        assertEquals("test", childListJson.get(1));
    }

    @Test
    public void testBasicRedaction() throws JSONException, IOException {
        Metadata metadata = new Metadata();
        metadata.setRedactKeys(Collections.singleton("password"));
        metadata.addMetadata("example", "password", "p4ssw0rd");
        metadata.addMetadata("example", "confirm_password", "p4ssw0rd");
        metadata.addMetadata("example", "normal", "safe");

        JSONObject metadataJson = streamableToJson(metadata);
        assertTrue(metadataJson.has("example"));

        JSONObject tabJson = metadataJson.getJSONObject("example");
        assertEquals("[FILTERED]", tabJson.getString("password"));
        assertEquals("[FILTERED]", tabJson.getString("confirm_password"));
        assertEquals("safe", tabJson.getString("normal"));
    }

    @Test
    public void testNestedRedaction() throws JSONException, IOException {
        Map<String, String> sensitiveMap = new HashMap<>();
        sensitiveMap.put("password", "p4ssw0rd");
        sensitiveMap.put("confirm_password", "p4ssw0rd");
        sensitiveMap.put("normal", "safe");

        Metadata metadata = new Metadata();
        metadata.setRedactKeys(Collections.singleton("password"));
        metadata.addMetadata("example", "sensitiveMap", sensitiveMap);

        JSONObject metadataJson = streamableToJson(metadata);
        assertTrue(metadataJson.has("example"));

        JSONObject tabJson = metadataJson.getJSONObject("example");
        JSONObject sensitiveMapJson = tabJson.getJSONObject("sensitiveMap");
        assertEquals("[FILTERED]", sensitiveMapJson.getString("password"));
        assertEquals("[FILTERED]", sensitiveMapJson.getString("confirm_password"));
        assertEquals("safe", sensitiveMapJson.getString("normal"));
    }

    @Test
    public void testFilterConstructor() throws Exception {
        Metadata metadata = new Metadata();
        metadata.addMetadata("foo", "password", "abc123");
        JSONObject jsonObject = streamableToJson(metadata);

        assertEquals(Collections.singleton("password"), metadata.getRedactKeys());
        assertEquals("[FILTERED]", jsonObject.getJSONObject("foo").get("password"));
    }

    @Test
    public void testClearTab() throws Exception {
        Metadata metadata = new Metadata();
        metadata.addMetadata("example", "string", "value");
        metadata.clearMetadata("example", null);
        JSONObject json = streamableToJson(metadata);
        assertFalse(json.has("example"));
    }
}
