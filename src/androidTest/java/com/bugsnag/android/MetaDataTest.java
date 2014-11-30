package com.bugsnag.android;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bugsnag.android.ExceptionChain;

public class MetaDataTest extends BugsnagTestCase {
    public void testBasicSerialization() throws JSONException {
        MetaData metaData = new MetaData();
        metaData.addToTab("example", "string", "value");
        metaData.addToTab("example", "integer", 123);
        metaData.addToTab("example", "double", 123.45);
        metaData.addToTab("example", "boolean", true);
        metaData.addToTab("example", "null", null);
        metaData.addToTab("example", "array", new String[]{"a", "b"});
        metaData.addToTab("example", "collection", Arrays.asList("Hello", "World"));

        Map<String, String> map = new HashMap<String, String>();
        map.put("key", "value");
        metaData.addToTab("example", "map", map);

        JSONObject metaDataJson = streamableToJson(metaData);
        assertTrue(metaDataJson.has("example"));

        JSONObject tab = metaDataJson.getJSONObject("example");
        assertEquals("value", tab.getString("string"));
        assertEquals(123, tab.getInt("integer"));
        assertEquals(123.45, tab.getDouble("double"));
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

    public void testNestedMapSerialization() throws JSONException {
        Map<String, String> childMap = new HashMap<String, String>();
        childMap.put("key", "value");

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("key", childMap);

        MetaData metaData = new MetaData();
        metaData.addToTab("example", "map", map);

        JSONObject metaDataJson = streamableToJson(metaData);
        assertTrue(metaDataJson.has("example"));

        JSONObject childMapJson = metaDataJson.getJSONObject("example").getJSONObject("map").getJSONObject("key");
        assertEquals("value", childMapJson.getString("key"));
    }

    public void testNestedCollectionSerialization() throws JSONException {
        Collection childList = new LinkedList<String>();
        childList.add("james");
        childList.add("test");

        Collection list = new LinkedList();
        list.add(childList);

        MetaData metaData = new MetaData();
        metaData.addToTab("example", "list", list);

        JSONObject metaDataJson = streamableToJson(metaData);
        assertTrue(metaDataJson.has("example"));

        JSONArray childListJson = metaDataJson.getJSONObject("example").getJSONArray("list").getJSONArray(0);
        assertEquals(2, childListJson.length());
        assertEquals("james", childListJson.get(0));
        assertEquals("test", childListJson.get(1));
    }
}
