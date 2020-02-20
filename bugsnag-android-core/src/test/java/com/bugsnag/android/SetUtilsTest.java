package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class SetUtilsTest {

    @Test
    public void nullInput() {
        assertNull(SetUtils.sanitiseSet(null));
    }

    @Test
    public void sanitiseSet() {
        Set<String> input = new HashSet<>();
        input.add(null);
        input.add("foo");
        assertEquals(2, input.size());

        Set<String> objects = SetUtils.sanitiseSet(input);
        assertEquals(1, objects.size());
        assertEquals("foo", objects.toArray(new String[]{})[0]);
    }
}
