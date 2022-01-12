package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

public class FeatureFlagTest {
    @Test
    public void mapEntryContractEquals() {
        FeatureFlag flag = new FeatureFlag("sample_group", "a");
        Map.Entry<String, String> entry = new SimpleImmutableEntry<>(flag);

        // we want to task our own equals method, so we write this by hand:
        assertTrue(flag.equals(entry));
    }

    @Test
    public void mapEntryContractHashCode() {
        FeatureFlag flag = new FeatureFlag("sample_group", "a");
        Map.Entry<String, String> entry = new SimpleImmutableEntry<>(flag);

        assertEquals(entry.hashCode(), flag.hashCode());
    }

    @Test
    public void featureFlagFromMapEntry() {
        Map.Entry<String, String> entry = new SimpleImmutableEntry<>("sample_group", "1");
        FeatureFlag flag = new FeatureFlag(entry);

        assertEquals("sample_group", flag.getName());
        assertEquals("1", flag.getVariant());
    }

    @Test
    public void featureFlagFromMapEntryWithNoValue() {
        Map.Entry<String, String> entry = new SimpleImmutableEntry<>("sample_group", null);
        FeatureFlag flag = new FeatureFlag(entry);

        assertEquals("sample_group", flag.getName());
        assertNull(flag.getVariant());
    }

    @Test
    public void featureFlagWithNullVariant() {
        FeatureFlag flag = new FeatureFlag("demo");
        assertEquals("demo", flag.getName());
        assertNull(flag.getVariant());
    }

    @Test(expected = NullPointerException.class)
    public void nullNameFails() {
        new FeatureFlag((String) null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void setValueFails() {
        FeatureFlag flag = new FeatureFlag("sample_group", "a");
        flag.setValue("b");
    }
}
