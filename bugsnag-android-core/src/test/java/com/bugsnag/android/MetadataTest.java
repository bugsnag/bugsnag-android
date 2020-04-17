package com.bugsnag.android;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class MetadataTest {

    @Test
    public void testBasicMerge() {
        Metadata base = new Metadata();
        base.addMetadata("example", "name", "bob");
        base.addMetadata("example", "awesome", false);

        Metadata overrides = new Metadata();
        base.addMetadata("example", "age", 30);
        base.addMetadata("example", "awesome", true);

        Metadata merged = Metadata.Companion.merge(base, overrides);
        Map<String, Object> tab = merged.getMetadata("example");
        assertEquals("bob", tab.get("name"));
        assertEquals(30, tab.get("age"));
        assertEquals(true, tab.get("awesome"));
    }

    @Test
    public void testDeepMerge() {
        Map<String, String> baseMap = new HashMap<>();
        baseMap.put("key", "fromBase");
        Metadata base = new Metadata();
        base.addMetadata("example", "map", baseMap);

        Map<String, String> overridesMap = new HashMap<>();
        baseMap.put("key", "fromOverrides");
        Metadata overrides = new Metadata();
        overrides.addMetadata("example", "map", overridesMap);

        Metadata merged = Metadata.Companion.merge(base, overrides);
        Map<String, Object> tab = merged.getMetadata("example");

        @SuppressWarnings("unchecked")
        Map<String, String> mergedMap = (Map<String, String>) tab.get("map");
        assertEquals("fromOverrides", mergedMap.get("key"));
    }
}
