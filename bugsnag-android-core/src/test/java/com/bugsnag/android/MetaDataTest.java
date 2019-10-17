package com.bugsnag.android;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class MetaDataTest {

    @Test
    public void testBasicMerge() {
        MetaData base = new MetaData();
        base.addMetadata("example", "name", "bob");
        base.addMetadata("example", "awesome", false);

        MetaData overrides = new MetaData();
        base.addMetadata("example", "age", 30);
        base.addMetadata("example", "awesome", true);

        MetaData merged = MetaData.merge(base, overrides);
        Map<String, Object> tab = (Map<String, Object>) merged.getMetadata("example", null);
        assertEquals("bob", tab.get("name"));
        assertEquals(30, tab.get("age"));
        assertEquals(true, tab.get("awesome"));
    }

    @Test
    public void testNullMerge() {
        MetaData base = new MetaData();
        base.addMetadata("example", "name", "bob");

        MetaData merged = MetaData.merge(base, null);
        Map<String, Object> tab = (Map<String, Object>) merged.getMetadata("example", null);
        assertEquals("bob", tab.get("name"));

        merged = MetaData.merge(null, base);
        tab = (Map<String, Object>) merged.getMetadata("example", null);
        assertEquals("bob", tab.get("name"));
    }

    @Test
    public void testDeepMerge() {
        Map<String, String> baseMap = new HashMap<>();
        baseMap.put("key", "fromBase");
        MetaData base = new MetaData();
        base.addMetadata("example", "map", baseMap);

        Map<String, String> overridesMap = new HashMap<>();
        baseMap.put("key", "fromOverrides");
        MetaData overrides = new MetaData();
        overrides.addMetadata("example", "map", overridesMap);

        MetaData merged = MetaData.merge(base, overrides);
        Map<String, Object> tab = (Map<String, Object>) merged.getMetadata("example", null);

        @SuppressWarnings("unchecked")
        Map<String, String> mergedMap = (Map<String, String>) tab.get("map");
        assertEquals("fromOverrides", mergedMap.get("key"));
    }
}
