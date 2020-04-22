package com.bugsnag.android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("ConstantConditions")
public class CollectionUtilsTest {

    @Test
    public void nullInput() {
        assertTrue(CollectionUtils.containsNullElements(null));
    }

    @Test
    public void sanitiseSet() {
        Set<String> input = new HashSet<>();
        input.add("foo");
        assertFalse(CollectionUtils.containsNullElements(input));
        input.add(null);
        assertTrue(CollectionUtils.containsNullElements(input));
    }
}
