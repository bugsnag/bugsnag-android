package com.bugsnag.android;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MetaDataMergeTest {

    @Test
    public void testConcurrentMapMerge() {
        MetaData.Companion.merge(generateMetaData(), generateMetaData());
    }

    /**
     * Generates a metadata object with a tab value containing a map with a null entry
     */
    private MetaData generateMetaData() {
        MetaData metaData = new MetaData();
        Map<Object, Object> nestedMap = new HashMap<>();
        metaData.addMetadata("foo", "bar", nestedMap);
        nestedMap.put("whoops", null);
        return metaData;
    }
}
