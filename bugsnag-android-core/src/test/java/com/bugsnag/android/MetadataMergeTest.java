package com.bugsnag.android;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MetadataMergeTest {

    @Test
    public void testConcurrentMapMerge() {
        Metadata.Companion.merge(generateMetaData(), generateMetaData());
    }

    /**
     * Generates a metadata object with a tab value containing a map with a null entry
     */
    private Metadata generateMetaData() {
        Metadata metaData = new Metadata();
        Map<Object, Object> nestedMap = new HashMap<>();
        metaData.addMetadata("foo", "bar", nestedMap);
        nestedMap.put("whoops", null);
        return metaData;
    }
}
