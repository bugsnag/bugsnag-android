package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MetaDataTest {

    private Client client;

    @Before
    public void setUp() throws Exception {
        client = BugsnagTestUtils.generateClient();
    }

    @After
    public void tearDown() throws Exception {
        Async.cancelTasks();
        client.getOrientationListener().disable();
    }

    @Test
    public void testBasicSerialization() throws JSONException, IOException {
        MetaData metaData = new MetaData();
        metaData.addToTab("example", "string", "value");
        metaData.addToTab("example", "integer", 123);
        metaData.addToTab("example", "double", 123.45);
        metaData.addToTab("example", "boolean", true);
        metaData.addToTab("example", "null", null);
        metaData.addToTab("example", "array", new String[]{"a", "b"});
        List<String> strings = Arrays.asList("Hello", "World");
        metaData.addToTab("example", "collection", strings);

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        metaData.addToTab("example", "map", map);

        JSONObject metaDataJson = streamableToJson(metaData);
        assertTrue(metaDataJson.has("example"));

        JSONObject tab = metaDataJson.getJSONObject("example");
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

        MetaData metaData = new MetaData();
        metaData.addToTab("example", "map", map);

        JSONObject metaDataJson = streamableToJson(metaData);
        assertTrue(metaDataJson.has("example"));

        JSONObject example = metaDataJson.getJSONObject("example");
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

        MetaData metaData = new MetaData();
        metaData.addToTab("example", "list", list);

        JSONObject metaDataJson = streamableToJson(metaData);
        assertTrue(metaDataJson.has("example"));

        JSONArray jsonArray = metaDataJson.getJSONObject("example").getJSONArray("list");
        JSONArray childListJson = jsonArray.getJSONArray(0);
        assertEquals(2, childListJson.length());
        assertEquals("james", childListJson.get(0));
        assertEquals("test", childListJson.get(1));
    }

    @Test
    public void testBasicMerge() {
        MetaData base = new MetaData();
        base.addToTab("example", "name", "bob");
        base.addToTab("example", "awesome", false);

        MetaData overrides = new MetaData();
        base.addToTab("example", "age", 30);
        base.addToTab("example", "awesome", true);

        MetaData merged = MetaData.merge(base, overrides);
        Map<String, Object> tab = merged.getTab("example");
        assertEquals("bob", tab.get("name"));
        assertEquals(30, tab.get("age"));
        assertEquals(true, tab.get("awesome"));
    }

    @Test
    public void testNullMerge() {
        MetaData base = new MetaData();
        base.addToTab("example", "name", "bob");

        MetaData merged = MetaData.merge(base, null);
        Map<String, Object> tab = merged.getTab("example");
        assertEquals("bob", tab.get("name"));

        merged = MetaData.merge(null, base);
        tab = merged.getTab("example");
        assertEquals("bob", tab.get("name"));
    }

    @Test
    public void testDeepMerge() {
        Map<String, String> baseMap = new HashMap<>();
        baseMap.put("key", "fromBase");
        MetaData base = new MetaData();
        base.addToTab("example", "map", baseMap);

        Map<String, String> overridesMap = new HashMap<>();
        baseMap.put("key", "fromOverrides");
        MetaData overrides = new MetaData();
        overrides.addToTab("example", "map", overridesMap);

        MetaData merged = MetaData.merge(base, overrides);
        Map<String, Object> tab = merged.getTab("example");

        @SuppressWarnings("unchecked")
        Map<String, String> mergedMap = (Map<String, String>) tab.get("map");
        assertEquals("fromOverrides", mergedMap.get("key"));
    }

    @Test
    public void testBasicFiltering() throws JSONException, IOException {
        MetaData metaData = new MetaData();
        metaData.setFilters("password");
        metaData.addToTab("example", "password", "p4ssw0rd");
        metaData.addToTab("example", "confirm_password", "p4ssw0rd");
        metaData.addToTab("example", "normal", "safe");

        JSONObject metaDataJson = streamableToJson(metaData);
        assertTrue(metaDataJson.has("example"));

        JSONObject tabJson = metaDataJson.getJSONObject("example");
        assertEquals("[FILTERED]", tabJson.getString("password"));
        assertEquals("[FILTERED]", tabJson.getString("confirm_password"));
        assertEquals("safe", tabJson.getString("normal"));
    }

    @Test
    public void testNestedFiltering() throws JSONException, IOException {
        Map<String, String> sensitiveMap = new HashMap<>();
        sensitiveMap.put("password", "p4ssw0rd");
        sensitiveMap.put("confirm_password", "p4ssw0rd");
        sensitiveMap.put("normal", "safe");

        MetaData metaData = new MetaData();
        metaData.setFilters("password");
        metaData.addToTab("example", "sensitiveMap", sensitiveMap);

        JSONObject metaDataJson = streamableToJson(metaData);
        assertTrue(metaDataJson.has("example"));

        JSONObject tabJson = metaDataJson.getJSONObject("example");
        JSONObject sensitiveMapJson = tabJson.getJSONObject("sensitiveMap");
        assertEquals("[FILTERED]", sensitiveMapJson.getString("password"));
        assertEquals("[FILTERED]", sensitiveMapJson.getString("confirm_password"));
        assertEquals("safe", sensitiveMapJson.getString("normal"));
    }

    @Test
    public void testFilterConstructor() throws Exception {
        MetaData metaData = client.getMetaData();
        metaData.addToTab("foo", "password", "abc123");
        JSONObject jsonObject = streamableToJson(metaData);

        assertArrayEquals(new String[]{"password"}, metaData.getFilters());
        assertEquals("[FILTERED]", jsonObject.getJSONObject("foo").get("password"));
    }

    @Test
    public void testFilterSetter() throws Exception {
        MetaData metaData = new MetaData();
        client.setMetaData(metaData);
        assertArrayEquals(new String[]{"password"}, metaData.getFilters());
    }

    @Test
    public void testFilterOverride() throws Exception {
        MetaData metaData = client.getMetaData();
        client.setFilters("test", "another");
        assertArrayEquals(new String[]{"test", "another"}, metaData.getFilters());
    }

    @Test
    public void testFilterMetadataOverride() throws Exception {
        MetaData data = new MetaData();
        data.setFilters("CUSTOM");
        client.setMetaData(data);
        assertArrayEquals(new String[]{"CUSTOM"}, data.getFilters());
    }

    @Test
    public void testClearTab() throws Exception {
        MetaData metaData = new MetaData();
        metaData.addToTab("example", "string", "value");
        metaData.clearTab("example");
        JSONObject json = streamableToJson(metaData);
        assertFalse(json.has("example"));
    }
}
