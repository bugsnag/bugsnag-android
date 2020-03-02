package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MetadataDeserializerTest {

    private Map<String, Object> map = new HashMap<>();

    /**
     * Generates a map for verifying the serializer
     */
    @Before
    public void setup() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 123);
        data.put("surname", "Bloggs");

        map.put("custom", data);
        map.put("data", Collections.singletonMap("optIn", true));
    }

    @Test
    public void deserialize() {
        Metadata metadata = new MetadataDeserializer().deserialize(map);
        assertEquals(123, metadata.getMetadata("custom", "id"));
        assertEquals("Bloggs", metadata.getMetadata("custom", "surname"));
        assertTrue((Boolean) metadata.getMetadata("data", "optIn"));
    }
}
