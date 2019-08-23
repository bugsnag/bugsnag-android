package com.bugsnag.android;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MetaDataTest {

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
}
