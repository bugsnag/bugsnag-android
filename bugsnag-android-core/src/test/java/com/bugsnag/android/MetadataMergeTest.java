package com.bugsnag.android;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MetadataMergeTest {

    @Test
    public void testConcurrentMapMerge() {
        Metadata.Companion.merge(generateMetadata(), generateMetadata());
    }

    /**
     * Generates a metadata object with a tab value containing a map with a null entry
     */
    private Metadata generateMetadata() {
        Metadata metadata = new Metadata();
        Map<Object, Object> nestedMap = new HashMap<>();
        metadata.addMetadata("foo", "bar", nestedMap);
        nestedMap.put("whoops", null);
        return metadata;
    }
}
