package com.bugsnag.android;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MetaDataMergeTest {

    @Test
    public void testConcurrentMapMerge() {
        MetaData metaData = new MetaData();

        Map<Object, Object> nestedMap = new HashMap<>();
        metaData.addToTab("foo", "bar", nestedMap);
        nestedMap.put("whoops", null);

        Map<String, Object> mergedMap = MetaData.merge(metaData, metaData).store;
        assertFalse(mergedMap.containsKey("whoops"));
    }
}
